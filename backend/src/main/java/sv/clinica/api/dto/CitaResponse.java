package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;

import java.time.LocalDate;
import java.time.LocalTime;

/** Resumen de una cita para el paciente. Nunca incluye el id interno: solo el código público. */
public record CitaResponse(
        String codigo,
        EstadoCita estado,
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
        String tratamiento,
        String odontologo) {

    public static CitaResponse from(Cita c) {
        return new CitaResponse(c.getCodigo(), c.getEstado(), c.getFecha(), c.getHoraInicio(), c.getHoraFin(),
                c.getTratamiento().getNombre(), c.getOdontologo().getNombre());
    }
}
