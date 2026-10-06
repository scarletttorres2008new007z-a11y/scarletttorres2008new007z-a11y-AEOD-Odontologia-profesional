package sv.clinica.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordRequest(
        @NotBlank(message = "Escribe la contraseña nueva.")
        String password) {
}
