package sv.clinica.api.dto;

import jakarta.validation.constraints.NotNull;

/** Activar o desactivar algo de la configuración de la agenda. Desactivar no borra nada. */
public record ActivacionRequest(
        @NotNull(message = "Indica si queda activo o desactivado.")
        Boolean activo) {
}
