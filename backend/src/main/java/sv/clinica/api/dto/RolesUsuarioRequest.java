package sv.clinica.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RolesUsuarioRequest(
        @NotNull(message = "Indica los roles del usuario.")
        List<String> roles) {
}
