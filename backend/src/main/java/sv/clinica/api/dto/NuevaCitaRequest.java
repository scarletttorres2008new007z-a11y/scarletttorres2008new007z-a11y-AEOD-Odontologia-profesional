package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cita que da la clínica desde el software a un paciente con ficha. El horario se elige entre los que devuelve
 * /api/agenda/disponibilidad y el backend vuelve a comprobar que sigue libre antes de guardarla. La duración la pone
 * el tratamiento: el cliente nunca la envía.
 */
public record NuevaCitaRequest(

        @NotNull(message = "Elige el tratamiento.")
        @Positive(message = "El tratamiento no es válido.")
        Long tratamientoId,

        @NotNull(message = "Elige un horario.")
        @Positive(message = "El odontólogo no es válido.")
        Long odontologoId,

        @NotNull(message = "Elige el día.")
        LocalDate fecha,

        @NotNull(message = "Elige un horario.")
        @Schema(type = "string", example = "10:30")
        LocalTime horaInicio,

        @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres.")
        @Schema(description = "Notas para el personal; el paciente no las ve")
        String notasInternas) {
}
