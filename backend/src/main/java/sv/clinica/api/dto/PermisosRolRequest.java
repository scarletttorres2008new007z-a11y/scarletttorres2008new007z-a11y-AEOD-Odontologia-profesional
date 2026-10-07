package sv.clinica.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PermisosRolRequest(
        @NotNull(message = "Indica los permisos del rol.")
        List<String> permisos) {
}
