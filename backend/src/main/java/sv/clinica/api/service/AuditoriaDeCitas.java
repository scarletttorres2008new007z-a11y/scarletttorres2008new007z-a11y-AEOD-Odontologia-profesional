package sv.clinica.api.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.OrigenCita;
import sv.clinica.api.event.CitaEvento;
import sv.clinica.api.repository.CitaRepository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deja en la auditoría cada reserva, cancelación y reprogramación, vengan de la landing, del software o (más adelante)
 * de la app. Guarda los datos de la cita y, si tiene ficha, el código del paciente; no sus datos personales, que ya
 * están en la propia cita y en la ficha.
 */
@Component
public class AuditoriaDeCitas {

    private final CitaRepository citas;
    private final AuditoriaService auditoria;

    public AuditoriaDeCitas(CitaRepository citas, AuditoriaService auditoria) {
        this.citas = citas;
        this.auditoria = auditoria;
    }

    @EventListener
    public void alCambiarCita(CitaEvento evento) {
        Cita cita = citas.findById(evento.citaId()).orElseThrow();
        switch (evento.tipo()) {
            case RESERVADA -> auditoria.registrar(origen(cita.getOrigen()), AccionAuditoria.RESERVAR,
                    EntidadAuditoria.CITA, cita.getId(), null, datos(cita));
            case CANCELADA -> {
                Map<String, Object> despues = new LinkedHashMap<>();
                despues.put("estado", EstadoCita.CANCELADA);
                if (cita.getMotivoCancelacion() != null) despues.put("motivo", cita.getMotivoCancelacion());
                auditoria.registrar(origen(cita.getCanceladaPor()), AccionAuditoria.CANCELAR, EntidadAuditoria.CITA,
                        cita.getId(), evento.estadoAnterior() == null ? null : Map.of("estado", evento.estadoAnterior()),
                        despues);
            }
            case REPROGRAMADA -> {
                Cita anterior = citas.findById(evento.citaAnteriorId()).orElseThrow();
                Map<String, Object> nueva = datos(cita);
                nueva.put("cita_id", cita.getId());
                auditoria.registrar(origen(cita.getOrigen()), AccionAuditoria.REPROGRAMAR,
                        EntidadAuditoria.CITA, anterior.getId(), datos(anterior), nueva);
            }
        }
    }

    private static Map<String, Object> datos(Cita c) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("estado", c.getEstado());
        datos.put("tratamiento", c.getTratamiento().getNombre());
        datos.put("odontologo", c.getOdontologo().getNombre());
        datos.put("fecha", c.getFecha().toString());
        datos.put("hora_inicio", c.getHoraInicio().toString());
        datos.put("hora_fin", c.getHoraFin().toString());
        if (c.getPaciente() != null) datos.put("paciente", c.getPaciente().getCodigo());
        return datos;
    }

    private static OrigenAuditoria origen(OrigenCita origen) {
        return switch (origen) {
            case LANDING -> OrigenAuditoria.LANDING;
            case SOFTWARE -> OrigenAuditoria.SOFTWARE;
            case APP -> OrigenAuditoria.APP;
        };
    }
}
