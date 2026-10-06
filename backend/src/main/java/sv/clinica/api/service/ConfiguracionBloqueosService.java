package sv.clinica.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.BloqueoGuardadoResponse;
import sv.clinica.api.dto.BloqueoRequest;
import sv.clinica.api.dto.BloqueoResponse;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.Bloqueo;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.BloqueoRepository;
import sv.clinica.api.repository.OdontologoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bloqueos de la agenda desde el software: festivos, vacaciones, reuniones, capacitaciones, mantenimiento, almuerzo
 * o cualquier otro rato no disponible. Se aplican al momento en la web y en el software. Las citas que ya había en ese
 * tiempo no se cancelan solas: se devuelven para que la clínica las mueva o las cancele. Quitar un bloqueo no lo
 * borra: queda desactivado y en la auditoría.
 */
@Service
public class ConfiguracionBloqueosService {

    private static final String NO_EXISTE = "Ese bloqueo no existe o ya se quitó.";

    private final BloqueoRepository bloqueos;
    private final OdontologoRepository odontologos;
    private final CitasAfectadas citasAfectadas;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ConfiguracionBloqueosService(BloqueoRepository bloqueos, OdontologoRepository odontologos,
                                        CitasAfectadas citasAfectadas, AuditoriaService auditoria, Clock clock) {
        this.bloqueos = bloqueos;
        this.odontologos = odontologos;
        this.citasAfectadas = citasAfectadas;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** Los que siguen en vigor: primero los que no tienen fechas (cada semana), después por fecha de inicio. */
    @Transactional(readOnly = true)
    public List<BloqueoResponse> listar() {
        return bloqueos.findVigentes(LocalDate.now(clock)).stream()
                .sorted(Comparator.comparing((Bloqueo b) -> b.getFechaInicio() != null)
                        .thenComparing(Bloqueo::getFechaInicio, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Bloqueo::getDiaSemana, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Bloqueo::getHoraInicio, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Bloqueo::getId))
                .map(BloqueoResponse::from)
                .toList();
    }

    @Transactional
    public BloqueoGuardadoResponse crear(BloqueoRequest request) {
        Datos d = revisar(request);
        Bloqueo bloqueo = new Bloqueo(request.tipo(), d.motivo, d.odontologo, d.fechaInicio, d.fechaFin,
                request.diaSemana(), d.horaInicio, d.horaFin);
        bloqueos.save(bloqueo);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CREAR, EntidadAuditoria.BLOQUEO, bloqueo.getId(),
                null, datosAuditables(bloqueo));
        return guardado(bloqueo);
    }

    @Transactional
    public BloqueoGuardadoResponse editar(Long id, BloqueoRequest request) {
        Bloqueo bloqueo = buscar(id);
        Datos d = revisar(request);
        Map<String, Object> antes = datosAuditables(bloqueo);
        bloqueo.editar(request.tipo(), d.motivo, d.odontologo, d.fechaInicio, d.fechaFin, request.diaSemana(),
                d.horaInicio, d.horaFin);
        Map<String, Object> despues = datosAuditables(bloqueo);
        antes.keySet().removeIf(clave -> Objects.equals(antes.get(clave), despues.get(clave)));
        despues.keySet().retainAll(antes.keySet());
        if (!antes.isEmpty()) {
            auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.EDITAR, EntidadAuditoria.BLOQUEO, id,
                    antes, despues);
        }
        return guardado(bloqueo);
    }

    /** Lo quita de la agenda (no se borra): ese tiempo vuelve a estar libre al momento. */
    @Transactional
    public void quitar(Long id) {
        Bloqueo bloqueo = buscar(id);
        bloqueo.setActivo(false);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.DESACTIVAR, EntidadAuditoria.BLOQUEO, id,
                datosAuditables(bloqueo), Map.of("activo", false));
    }

    private Bloqueo buscar(Long id) {
        return bloqueos.findById(id).filter(Bloqueo::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(NO_EXISTE));
    }

    private BloqueoGuardadoResponse guardado(Bloqueo bloqueo) {
        List<Cita> afectadas = citasAfectadas.buscar(
                c -> bloqueo.coincideCon(c.getOdontologo(), c.getFecha(), c.getHoraInicio(), c.getHoraFin()));
        return new BloqueoGuardadoResponse(BloqueoResponse.from(bloqueo), citasAfectadas.respuesta(afectadas));
    }

    /* ─────────────── Validación ─────────────── */

    private record Datos(String motivo, Odontologo odontologo, LocalDate fechaInicio, LocalDate fechaFin,
                         LocalTime horaInicio, LocalTime horaFin) {
    }

    private Datos revisar(BloqueoRequest r) {
        Odontologo odontologo = r.odontologoId() == null ? null
                : odontologos.findByIdAndActivoTrue(r.odontologoId()).orElseThrow(() ->
                new DatosInvalidosException("odontologo_id", "Ese odontólogo no existe o está desactivado."));

        LocalDate inicio = r.fechaInicio();
        LocalDate fin = r.fechaFin();
        if (inicio == null && fin != null) {
            throw new DatosInvalidosException("fecha_inicio", "Indica también el primer día.");
        }
        if (inicio == null && r.diaSemana() == null) {
            throw new DatosInvalidosException("fecha_inicio", "Indica el día (o los días) o un día de la semana.");
        }
        // Solo con fechas, sin último día, es ese único día; cada semana, sin último día, sigue sin fin
        if (inicio != null && fin == null && r.diaSemana() == null) fin = inicio;
        if (fin != null && fin.isBefore(inicio)) {
            throw new DatosInvalidosException("fecha_fin", "El último día no puede ser anterior al primero.");
        }
        if (fin != null && fin.isBefore(LocalDate.now(clock))) {
            throw new DatosInvalidosException("fecha_fin", "Esas fechas ya han pasado: elige días de hoy en adelante.");
        }

        LocalTime horaInicio = r.horaInicio();
        LocalTime horaFin = r.horaFin();
        if ((horaInicio == null) != (horaFin == null)) {
            throw new DatosInvalidosException(horaInicio == null ? "hora_inicio" : "hora_fin",
                    "Indica la hora de inicio y la de fin, o ninguna para bloquear el día entero.");
        }
        if (horaInicio != null) {
            HorasDeAgenda.comprobarCuartoDeHora(horaInicio, "hora_inicio");
            HorasDeAgenda.comprobarCuartoDeHora(horaFin, "hora_fin");
            if (!horaFin.isAfter(horaInicio)) {
                throw new DatosInvalidosException("hora_fin", "La hora de fin tiene que ser posterior a la de inicio.");
            }
        }
        return new Datos(Textos.opcional(r.motivo()), odontologo, inicio, fin, horaInicio, horaFin);
    }

    /* ─────────────── Auditoría ─────────────── */

    private static Map<String, Object> datosAuditables(Bloqueo b) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tipo", b.getTipo().name());
        datos.put("motivo", b.getMotivo());
        datos.put("odontologo", b.getOdontologo() == null ? "Toda la clínica" : b.getOdontologo().getNombre());
        datos.put("fecha_inicio", b.getFechaInicio() == null ? null : b.getFechaInicio().toString());
        datos.put("fecha_fin", b.getFechaFin() == null ? null : b.getFechaFin().toString());
        datos.put("dia_semana", b.getDiaSemana() == null ? null : HorasDeAgenda.dia(b.getDiaSemana()));
        datos.put("horas", b.esDiaCompleto() ? "Todo el día" : HorasDeAgenda.tramo(b.getHoraInicio(), b.getHoraFin()));
        return datos;
    }
}
