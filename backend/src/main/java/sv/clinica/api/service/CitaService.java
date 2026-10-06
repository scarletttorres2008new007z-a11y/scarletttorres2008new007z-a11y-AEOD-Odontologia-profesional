package sv.clinica.api.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.config.CitasProperties;
import sv.clinica.api.dto.CitaDetalleResponse;
import sv.clinica.api.dto.CitaRequest;
import sv.clinica.api.dto.CitaResponse;
import sv.clinica.api.dto.NuevaCitaRequest;
import sv.clinica.api.dto.ReprogramacionRequest;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OcupacionAgenda;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.OrigenCita;
import sv.clinica.api.entity.Paciente;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.event.CitaEvento;
import sv.clinica.api.exception.CitaNoModificableException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.HorarioNoDisponibleException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.CitaRepository;
import sv.clinica.api.repository.OcupacionAgendaRepository;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.repository.PacienteRepository;
import sv.clinica.api.repository.TratamientoRepository;
import sv.clinica.api.repository.UsuarioRepository;
import sv.clinica.api.security.UsuarioAutenticado;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ciclo de vida de una cita: reservar o dar cita, confirmar y demás estados, cancelar y reprogramar.
 * Es la misma lógica para la landing, el software y la futura app: cambia solo el {@link OrigenCita}.
 *
 * Doble reserva, dos barreras:
 *   1. Antes de guardar se recalcula la disponibilidad del hueco.
 *   2. La cita se guarda con sus tramos en agenda_ocupacion, cuya clave única impide
 *      en la base de datos que dos citas se solapen aunque lleguen a la vez.
 */
@Service
public class CitaService {

    private static final Map<EstadoCita, String> ESTADOS = Map.of(
            EstadoCita.PENDIENTE, "pendiente de confirmar",
            EstadoCita.CONFIRMADA, "confirmada",
            EstadoCita.EN_ATENCION, "en consulta",
            EstadoCita.COMPLETADA, "completada",
            EstadoCita.NO_ASISTIO, "marcada como «no asistió»",
            EstadoCita.CANCELADA, "cancelada",
            EstadoCita.REPROGRAMADA, "reprogramada");

    private final CitaRepository citas;
    private final OcupacionAgendaRepository ocupacion;
    private final TratamientoRepository tratamientos;
    private final OdontologoRepository odontologos;
    private final PacienteRepository pacientes;
    private final UsuarioRepository usuarios;
    private final DisponibilidadService disponibilidad;
    private final AccesoACitas acceso;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;
    private final CitasProperties reglas;
    private final Clock clock;

    public CitaService(CitaRepository citas, OcupacionAgendaRepository ocupacion, TratamientoRepository tratamientos,
                       OdontologoRepository odontologos, PacienteRepository pacientes, UsuarioRepository usuarios,
                       DisponibilidadService disponibilidad, AccesoACitas acceso, AuditoriaService auditoria,
                       ApplicationEventPublisher eventos, CitasProperties reglas, Clock clock) {
        this.citas = citas;
        this.ocupacion = ocupacion;
        this.tratamientos = tratamientos;
        this.odontologos = odontologos;
        this.pacientes = pacientes;
        this.usuarios = usuarios;
        this.disponibilidad = disponibilidad;
        this.acceso = acceso;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.reglas = reglas;
        this.clock = clock;
    }

    /* ─────────────── Paciente (landing y futura app) ─────────────── */

    /** Reserva el horario elegido. Lanza HorarioNoDisponibleException (409) si ya no está libre. */
    @Transactional
    public Cita reservar(CitaRequest request, OrigenCita origen) {
        Tratamiento tratamiento = tratamientoActivo(request.tratamientoId());
        Odontologo odontologo = odontologoActivo(request.odontologoId());
        comprobarQueLoRealiza(tratamiento, odontologo);
        disponibilidad.validarFecha(request.fecha(), "fecha");

        Cita cita = new Cita(tratamiento, odontologo, request.fecha(), request.horaInicio(),
                DisponibilidadService.duracionDe(tratamiento),
                request.nombre().trim(), request.telefono().trim(), request.email().trim(),
                Textos.opcional(request.mensaje()), estadoInicial(origen), origen, ahora());
        guardarEnAgenda(cita, origen);
        eventos.publishEvent(CitaEvento.reservada(cita.getId()));
        return cita;
    }

    /**
     * Cancela una cita por su código: no se borra, pasa a CANCELADA y su hueco vuelve a estar libre.
     * El paciente (landing o app) solo puede hacerlo con la antelación configurada; la clínica, siempre.
     */
    @Transactional
    public Cita cancelar(String codigo, OrigenCita solicitante, String motivo) {
        return cancelar(porCodigo(codigo), solicitante, motivo);
    }

    /** La cancelación que pide el propio paciente, con la respuesta ya preparada dentro de la transacción. */
    @Transactional
    public CitaResponse cancelarComoPaciente(String codigo, OrigenCita solicitante, String motivo) {
        return CitaResponse.from(cancelar(porCodigo(codigo), solicitante, motivo));
    }

    /**
     * Mueve una cita a otro horario en un solo paso: la antigua pasa a REPROGRAMADA (y libera su hueco)
     * y se crea la nueva enlazada a ella. Si el nuevo horario no está libre, no cambia nada.
     */
    @Transactional
    public Cita reprogramar(String codigo, Long odontologoId, LocalDate fecha, LocalTime horaInicio, OrigenCita solicitante) {
        return reprogramar(porCodigo(codigo), odontologoId, fecha, horaInicio, solicitante);
    }

    /** Regla de antelación para el paciente. La usará la app para mostrar u ocultar el botón "Cancelar". */
    public boolean puedeModificar(Cita cita, OrigenCita solicitante) {
        if (!cita.getEstado().esModificable()) return false;
        if (!solicitante.esPaciente()) return true;
        return ahora().isBefore(cita.getInicio().minusHours(reglas.cancelacionAntelacionHoras()));
    }

    /* ─────────────── Software de gestión ─────────────── */

    /** La clínica da cita a un paciente con ficha. Queda CONFIRMADA y a nombre de quien la da. */
    @Transactional
    public CitaDetalleResponse darCita(Long pacienteId, NuevaCitaRequest request) {
        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El paciente no existe."));
        if (!paciente.isActivo()) {
            throw new CitaNoModificableException("Este paciente está de baja. Reactívalo desde su ficha para darle cita.");
        }
        Tratamiento tratamiento = tratamientoActivo(request.tratamientoId());
        Odontologo odontologo = odontologoActivo(request.odontologoId());
        comprobarQueLoRealiza(tratamiento, odontologo);
        comprobarAgendaPropia(odontologo);
        disponibilidad.validarFecha(request.fecha(), "fecha");

        Cita cita = Cita.paraPaciente(paciente, tratamiento, odontologo, request.fecha(), request.horaInicio(),
                DisponibilidadService.duracionDe(tratamiento), Textos.opcional(request.notasInternas()),
                estadoInicial(OrigenCita.SOFTWARE), usuarioActual(), ahora());
        guardarEnAgenda(cita, OrigenCita.SOFTWARE);
        eventos.publishEvent(CitaEvento.reservada(cita.getId()));
        return detalle(cita);
    }

    /** Confirmar, en consulta, completada o no asistió. Cancelar y reprogramar tienen sus propios métodos. */
    @Transactional
    public CitaDetalleResponse cambiarEstado(Long id, EstadoCita nuevo) {
        Cita cita = visible(id);
        EstadoCita anterior = cita.getEstado();
        if (anterior == nuevo) return detalle(cita);
        if (nuevo == EstadoCita.CANCELADA || nuevo == EstadoCita.REPROGRAMADA) {
            throw new DatosInvalidosException("estado", "Para cancelar o reprogramar una cita usa esas opciones.");
        }
        if (!anterior.siguientes().contains(nuevo)) {
            throw new CitaNoModificableException("Esta cita está " + ESTADOS.get(anterior)
                    + " y no se puede pasar a " + ESTADOS.get(nuevo) + ".");
        }
        if (!cita.estadosSiguientes(hoy()).contains(nuevo)) {
            throw new CitaNoModificableException("Eso solo se puede marcar a partir del día de la cita.");
        }
        cita.cambiarEstado(nuevo, ahora());
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CAMBIAR_ESTADO, EntidadAuditoria.CITA, cita.getId(),
                Map.of("estado", anterior), Map.of("estado", nuevo));
        return detalle(cita);
    }

    @Transactional
    public CitaDetalleResponse cancelarDesdeLaClinica(Long id, String motivo) {
        return detalle(cancelar(visible(id), OrigenCita.SOFTWARE, motivo));
    }

    /** Devuelve la cita nueva. La anterior queda REPROGRAMADA y enlazada a ella. */
    @Transactional
    public CitaDetalleResponse reprogramarDesdeLaClinica(Long id, ReprogramacionRequest request) {
        Cita anterior = visible(id);
        Odontologo odontologo = odontologoActivo(request.odontologoId());
        comprobarAgendaPropia(odontologo);
        return detalle(reprogramar(anterior, odontologo.getId(), request.fecha(), request.horaInicio(),
                OrigenCita.SOFTWARE));
    }

    /** Vincula la cita a la ficha de un paciente (por ejemplo, la de una reserva de la web). */
    @Transactional
    public CitaDetalleResponse vincularPaciente(Long id, Long pacienteId) {
        Cita cita = visible(id);
        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new DatosInvalidosException("paciente_id", "El paciente no existe."));
        if (!paciente.isActivo()) {
            throw new DatosInvalidosException("paciente_id",
                    "Este paciente está de baja. Reactívalo desde su ficha para vincularle citas.");
        }
        Paciente anterior = cita.getPaciente();
        if (paciente.equals(anterior)) return detalle(cita);
        cita.vincularPaciente(paciente, ahora());
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.VINCULAR_PACIENTE, EntidadAuditoria.CITA,
                cita.getId(), anterior == null ? null : Map.of("paciente", anterior.getCodigo()),
                Map.of("paciente", paciente.getCodigo()));
        return detalle(cita);
    }

    @Transactional
    public CitaDetalleResponse cambiarNotas(Long id, String notas) {
        Cita cita = visible(id);
        String anteriores = cita.getNotasInternas();
        String nuevas = Textos.opcional(notas);
        if (Objects.equals(anteriores, nuevas)) return detalle(cita);
        cita.cambiarNotasInternas(nuevas, ahora());
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.EDITAR, EntidadAuditoria.CITA, cita.getId(),
                anteriores == null ? null : Map.of("notas_internas", anteriores),
                nuevas == null ? null : Map.of("notas_internas", nuevas));
        return detalle(cita);
    }

    /** La cita completa para el personal, si quien pregunta puede ver su agenda (si no, 404). */
    @Transactional(readOnly = true)
    public CitaDetalleResponse obtener(Long id) {
        return detalle(visible(id));
    }

    /* ─────────────── Internos ─────────────── */

    private Cita cancelar(Cita cita, OrigenCita solicitante, String motivo) {
        comprobarModificable(cita, solicitante, "cancelar");
        EstadoCita anterior = cita.getEstado();
        cita.cancelar(solicitante, Textos.opcional(motivo), ahora());
        ocupacion.liberar(cita);
        eventos.publishEvent(CitaEvento.cancelada(cita.getId(), anterior));
        return cita;
    }

    private Cita reprogramar(Cita anterior, Long odontologoId, LocalDate fecha, LocalTime horaInicio,
                             OrigenCita solicitante) {
        comprobarModificable(anterior, solicitante, "reprogramar");
        if (anterior.getOdontologo().getId().equals(odontologoId) && anterior.getFecha().equals(fecha)
                && anterior.getHoraInicio().equals(horaInicio)) {
            throw new DatosInvalidosException("hora_inicio", "La cita ya está a esa hora. Elige otro horario.");
        }
        Odontologo odontologo = odontologoActivo(odontologoId);
        comprobarQueLoRealiza(anterior.getTratamiento(), odontologo);
        disponibilidad.validarFecha(fecha, "fecha");

        anterior.marcarReprogramada(ahora());
        ocupacion.liberar(anterior);
        citas.flush();

        Cita nueva = new Cita(anterior.getTratamiento(), odontologo, fecha, horaInicio,
                DisponibilidadService.duracionDe(anterior.getTratamiento()),
                anterior.getNombre(), anterior.getTelefono(), anterior.getEmail(), anterior.getMensaje(),
                estadoInicial(solicitante), solicitante, ahora());
        nueva.heredarDe(anterior, solicitante == OrigenCita.SOFTWARE ? usuarioActual() : null);
        guardarEnAgenda(nueva, solicitante);
        eventos.publishEvent(CitaEvento.reprogramada(nueva.getId(), anterior.getId()));
        return nueva;
    }

    private void guardarEnAgenda(Cita cita, OrigenCita origen) {
        // Barrera 1: el hueco tiene que seguir libre según el cálculo de disponibilidad
        if (!disponibilidad.estaDisponible(cita.getTratamiento(), cita.getOdontologo(), cita.getFecha(),
                cita.getHoraInicio(), origen)) {
            throw new HorarioNoDisponibleException();
        }
        // Barrera 2: un registro por tramo de 15 min con clave única (odontólogo, fecha, hora)
        List<OcupacionAgenda> tramos = new ArrayList<>();
        for (LocalTime t = cita.getHoraInicio(); t.isBefore(cita.getHoraFin()); t = t.plusMinutes(OcupacionAgenda.MINUTOS_TRAMO)) {
            tramos.add(new OcupacionAgenda(cita, t));
        }
        try {
            citas.save(cita);
            ocupacion.saveAllAndFlush(tramos);
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException carrera) {
            // Otra reserva se llevó el mismo hueco a la vez: la base de datos rechazó la segunda
            throw new HorarioNoDisponibleException();
        }
    }

    private void comprobarModificable(Cita cita, OrigenCita solicitante, String accion) {
        EstadoCita estado = cita.getEstado();
        if (estado == EstadoCita.CANCELADA || estado == EstadoCita.REPROGRAMADA) {
            throw new CitaNoModificableException("Esta cita ya no está activa y no se puede " + accion + ".");
        }
        if (!estado.esModificable()) {
            throw new CitaNoModificableException("Esta cita ya está " + ESTADOS.get(estado) + " y no se puede " + accion + ".");
        }
        if (!puedeModificar(cita, solicitante)) {
            throw new CitaNoModificableException("Solo se puede " + accion + " con al menos "
                    + reglas.cancelacionAntelacionHoras() + " horas de antelación. Llama a la clínica.");
        }
    }

    /** Quien solo ve su agenda solo puede dar o mover citas en ella. */
    private void comprobarAgendaPropia(Odontologo odontologo) {
        if (!acceso.delUsuarioActual().puedeVer(odontologo)) {
            throw new DatosInvalidosException("odontologo_id", "Solo puedes dar o mover citas en tu agenda.");
        }
    }

    private void comprobarQueLoRealiza(Tratamiento tratamiento, Odontologo odontologo) {
        if (!tratamiento.loRealiza(odontologo)) {
            throw new DatosInvalidosException("odontologo_id", "Este odontólogo no realiza el tratamiento seleccionado.");
        }
    }

    private Tratamiento tratamientoActivo(Long id) {
        return tratamientos.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new DatosInvalidosException("tratamiento_id", "El tratamiento seleccionado no existe."));
    }

    private Odontologo odontologoActivo(Long id) {
        return odontologos.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new DatosInvalidosException("odontologo_id", "El odontólogo seleccionado no existe."));
    }

    private Cita porCodigo(String codigo) {
        return citas.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ninguna cita con ese código."));
    }

    private Cita visible(Long id) {
        return citas.findById(id).map(acceso::comprobar)
                .orElseThrow(() -> new RecursoNoEncontradoException(AccesoACitas.CITA_NO_EXISTE));
    }

    private CitaDetalleResponse detalle(Cita cita) {
        Long nueva = cita.getEstado() == EstadoCita.REPROGRAMADA ? citas.findIdDeLaNueva(cita.getId()).orElse(null) : null;
        return CitaDetalleResponse.from(cita, nueva, cita.estadosSiguientes(hoy()));
    }

    private EstadoCita estadoInicial(OrigenCita origen) {
        return reglas.confirmacionAutomatica() || origen == OrigenCita.SOFTWARE ? EstadoCita.CONFIRMADA : EstadoCita.PENDIENTE;
    }

    private Usuario usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuarios.getReferenceById(usuario.id());
        }
        return null;
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
