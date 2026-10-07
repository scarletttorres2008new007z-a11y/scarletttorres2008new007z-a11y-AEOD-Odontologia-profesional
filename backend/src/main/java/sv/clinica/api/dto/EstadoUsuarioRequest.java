package sv.clinica.api.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoUsuarioRequest(
        @NotNull(message = "Indica si el usuario queda activo o no.")
        Boolean activo) {
}
