package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/** Nuevo horario para una cita, elegido entre los de /api/agenda/disponibilidad. El tratamiento no cambia. */
public record ReprogramacionRequest(

        @NotNull(message = "Elige un horario.")
        @Positive(message = "El odontólogo no es válido.")
        Long odontologoId,

        @NotNull(message = "Elige el día.")
        LocalDate fecha,

        @NotNull(message = "Elige un horario.")
        @Schema(type = "string", example = "10:30")
        LocalTime horaInicio) {
}
