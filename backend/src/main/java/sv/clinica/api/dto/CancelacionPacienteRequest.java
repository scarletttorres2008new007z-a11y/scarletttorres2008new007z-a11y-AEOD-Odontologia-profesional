package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cancelación por el propio paciente (landing). Se identifica con el código de la cita, que solo recibe quien la
 * reservó: es aleatorio y no se puede adivinar.
 */
public record CancelacionPacienteRequest(
        @NotBlank(message = "Falta el código de la cita.")
        @Size(max = 36, message = "El código de la cita no es válido.")
        @Schema(description = "El código que devolvió la reserva")
        String codigo) {
}
