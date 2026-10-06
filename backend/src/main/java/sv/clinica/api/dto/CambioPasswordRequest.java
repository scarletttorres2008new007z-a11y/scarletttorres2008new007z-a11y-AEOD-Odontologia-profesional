package sv.clinica.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CambioPasswordRequest(
        @NotBlank(message = "Escribe tu contraseña actual.")
        String passwordActual,

        @NotBlank(message = "Escribe la contraseña nueva.")
        String passwordNueva) {
}
