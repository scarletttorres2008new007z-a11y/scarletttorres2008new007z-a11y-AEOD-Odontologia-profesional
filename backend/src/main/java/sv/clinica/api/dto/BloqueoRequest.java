package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import sv.clinica.api.entity.TipoBloqueo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Nuevo bloqueo o cambio de uno. Hay que indicar las fechas, el día de la semana o las dos cosas
 * (por ejemplo, «cada miércoles de octubre»). Sin horas, bloquea el día entero.
 */
public record BloqueoRequest(

        @NotNull(message = "Elige el tipo de bloqueo.")
        TipoBloqueo tipo,

        @Size(max = 150, message = "El motivo no puede superar los 150 caracteres.")
        String motivo,

        @Positive(message = "El odontólogo no es válido.")
        @Schema(description = "Vacío = toda la clínica")
        Long odontologoId,

        @Schema(description = "Primer día. Vacío = desde siempre (solo con día de la semana)")
        LocalDate fechaInicio,

        @Schema(description = "Último día, incluido. Vacío = solo el primer día; con día de la semana, sin fin")
        LocalDate fechaFin,

        @Min(value = 1, message = "El día de la semana no es válido.")
        @Max(value = 7, message = "El día de la semana no es válido.")
        @Schema(description = "Cada semana ese día: 1 = lunes … 7 = domingo")
        Integer diaSemana,

        @Schema(type = "string", example = "09:00", description = "Vacías las dos horas = el día entero")
        LocalTime horaInicio,

        @Schema(type = "string", example = "10:00")
        LocalTime horaFin) {
}
