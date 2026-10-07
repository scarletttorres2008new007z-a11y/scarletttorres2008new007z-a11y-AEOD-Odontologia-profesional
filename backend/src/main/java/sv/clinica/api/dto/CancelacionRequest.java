package sv.clinica.api.dto;

import jakarta.validation.constraints.Size;

/** Cancelación desde el software. El motivo es opcional y queda en la cita y en la auditoría. */
public record CancelacionRequest(
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.")
        String motivo) {
}
