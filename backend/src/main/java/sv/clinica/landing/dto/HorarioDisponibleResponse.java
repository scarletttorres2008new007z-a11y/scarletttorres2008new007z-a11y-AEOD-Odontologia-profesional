package sv.clinica.landing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import sv.clinica.landing.service.DisponibilidadService.Horario;

import java.time.LocalDate;
import java.time.LocalTime;

public record HorarioDisponibleResponse(
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
        Long odontologoId,
        String odontologoNombre,
        String odontologoEspecialidad) {

    public static HorarioDisponibleResponse from(Horario h) {
        return new HorarioDisponibleResponse(h.fecha(), h.horaInicio(), h.horaFin(),
                h.odontologo().getId(), h.odontologo().getNombre(), h.odontologo().getEspecialidad());
    }
}
