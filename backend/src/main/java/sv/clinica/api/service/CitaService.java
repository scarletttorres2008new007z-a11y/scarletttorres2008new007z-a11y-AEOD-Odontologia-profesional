package sv.clinica.api.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.config.CitasProperties;
import sv.clinica.api.dto.CitaRequest;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OcupacionAgenda;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.OrigenCita;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.event.CitaEvento;
import sv.clinica.api.exception.CitaNoModificableException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.HorarioNoDisponibleException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.CitaRepository;
import sv.clinica.api.repository.OcupacionAgendaRepository;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.repository.TratamientoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ciclo de vida de una cita: reservar, cancelar y reprogramar.
 * Es la misma lógica que usarán en el futuro el software de clínica y la app del paciente
 * (cambia solo el {@link OrigenCita}).
 *
 * Doble reserva, dos barreras:
 *   1. Antes de guardar se recalcula la disponibilidad del hueco.
 *   2. La cita se guarda con sus tramos en agenda_ocupacion, cuya clave única impide
 *      en la base de datos que dos citas se solapen aunque lleguen a la vez.
 */
@Service
public class CitaService {

    private final CitaRepository citas;
    private final OcupacionAgendaRepository ocupacion;
    private final TratamientoRepository tratamientos;
    private final OdontologoRepository odontologos;
    private final DisponibilidadService disponibilidad;
    private final ApplicationEventPublisher eventos;
    private final CitasProperties reglas;
    private final Clock clock;

    public CitaService(CitaRepository citas, OcupacionAgendaRepository ocupacion, TratamientoRepository tratamientos,
                       OdontologoRepository odontologos, DisponibilidadService disponibilidad,
                       ApplicationEventPublisher eventos, CitasProperties reglas, Clock clock) {
        this.citas = citas;
        this.ocupacion = ocupacion;
        this.tratamientos = tratamientos;
        this.odontologos = odontologos;
        this.disponibilidad = disponibilidad;
        this.eventos = eventos;
        this.reglas = reglas;
        this.clock = clock;
    }

    /** Reserva el horario elegido. Lanza HorarioNoDisponibleException (409) si ya no está libre. */
    @Transactional
    public Cita reservar(CitaRequest request, OrigenCita origen) {
        Tratamiento tratamiento = tratamientos.findByIdAndActivoTrue(request.tratamientoId())
                .orElseThrow(() -> new DatosInvalidosException("tratamiento_id", "El tratamiento seleccionado no existe."));
        Odontologo odontologo = odontologos.findByIdAndActivoTrue(request.odontologoId())
                .orElseThrow(() -> new DatosInvalidosException("odontologo_id", "El odontólogo seleccionado no existe."));
        if (!tratamiento.loRealiza(odontologo)) {
            throw new DatosInvalidosException("odontologo_id", "Este odontólogo no realiza el tratamiento seleccionado.");
        }
        disponibilidad.validarFecha(request.fecha(), "fecha");

        Cita cita = new Cita(tratamiento, odontologo, request.fecha(), request.horaInicio(),
                DisponibilidadService.duracionDe(tratamiento),
                request.nombre().trim(), request.telefono().trim(), request.email().trim(),
                Textos.opcional(request.mensaje()), estadoInicial(origen), origen, ahora());
        guardarEnAgenda(cita);
        eventos.publishEvent(CitaEvento.reservada(cita.getId()));
        return cita;
    }

    /**
     * Cancela una cita: no se borra, pasa a CANCELADA y su hueco vuelve a estar libre.
     * El paciente (landing o app) solo puede hacerlo con la antelación configurada; la clínica, siempre.
     */
    @Transactional
    public Cita cancelar(String codigo, OrigenCita solicitante, String motivo) {
        Cita cita = buscar(codigo);
        comprobarModificable(cita, solicitante, "cancelar");
        cita.cancelar(solicitante, Textos.opcional(motivo), ahora());
        ocupacion.liberar(cita);
        eventos.publishEvent(CitaEvento.cancelada(cita.getId()));
        return cita;
    }

    /**
     * Mueve una cita a otro horario en un solo paso: la antigua pasa a REPROGRAMADA (y libera su hueco)
     * y se crea la nueva enlazada a ella. Si el nuevo horario no está libre, no cambia nada.
     */
    @Transactional
    public Cita reprogramar(String codigo, Long odontologoId, LocalDate fecha, LocalTime horaInicio, OrigenCita solicitante) {
        Cita anterior = buscar(codigo);
        comprobarModificable(anterior, solicitante, "reprogramar");
        Odontologo odontologo = odontologos.findByIdAndActivoTrue(odontologoId)
                .orElseThrow(() -> new DatosInvalidosException("odontologo_id", "El odontólogo seleccionado no existe."));
        disponibilidad.validarFecha(fecha, "fecha");

        anterior.marcarReprogramada(ahora());
        ocupacion.liberar(anterior);
        citas.flush();

        Cita nueva = new Cita(anterior.getTratamiento(), odontologo, fecha, horaInicio,
                DisponibilidadService.duracionDe(anterior.getTratamiento()),
                anterior.getNombre(), anterior.getTelefono(), anterior.getEmail(), anterior.getMensaje(),
                estadoInicial(solicitante), solicitante, ahora());
        nueva.setCitaAnterior(anterior);
        guardarEnAgenda(nueva);
        eventos.publishEvent(CitaEvento.reprogramada(nueva.getId(), anterior.getId()));
        return nueva;
    }

    /** Regla de antelación para el paciente. La usará la app para mostrar u ocultar el botón "Cancelar". */
    public boolean puedeModificar(Cita cita, OrigenCita solicitante) {
        if (!cita.getEstado().ocupaAgenda()) return false;
        if (!solicitante.esPaciente()) return true;
        return ahora().isBefore(cita.getInicio().minusHours(reglas.cancelacionAntelacionHoras()));
    }

    /* ─────────────── Internos ─────────────── */

    private void guardarEnAgenda(Cita cita) {
        // Barrera 1: el hueco tiene que seguir libre según el cálculo de disponibilidad
        if (!disponibilidad.estaDisponible(cita.getTratamiento(), cita.getOdontologo(), cita.getFecha(), cita.getHoraInicio())) {
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
        if (!cita.getEstado().ocupaAgenda()) {
            throw new CitaNoModificableException("Esta cita ya no está activa y no se puede " + accion + ".");
        }
        if (!puedeModificar(cita, solicitante)) {
            throw new CitaNoModificableException("Solo se puede " + accion + " con al menos "
                    + reglas.cancelacionAntelacionHoras() + " horas de antelación. Llama a la clínica.");
        }
    }

    private Cita buscar(String codigo) {
        return citas.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ninguna cita con ese código."));
    }

    private EstadoCita estadoInicial(OrigenCita origen) {
        return reglas.confirmacionAutomatica() || origen == OrigenCita.CLINICA ? EstadoCita.CONFIRMADA : EstadoCita.PENDIENTE;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
