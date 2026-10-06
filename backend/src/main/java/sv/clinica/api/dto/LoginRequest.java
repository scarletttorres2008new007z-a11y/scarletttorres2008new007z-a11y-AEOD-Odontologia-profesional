package sv.clinica.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Escribe tu usuario o tu correo.")
        @Size(max = 150, message = "El usuario o correo es demasiado largo.")
        String usuario,

        @NotBlank(message = "Escribe tu contraseña.")
        @Size(max = 200, message = "La contraseña es demasiado larga.")
        String password) {
}
