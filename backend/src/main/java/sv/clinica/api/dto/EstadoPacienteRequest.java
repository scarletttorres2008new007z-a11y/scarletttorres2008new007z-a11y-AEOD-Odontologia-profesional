package sv.clinica.api.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoPacienteRequest(
        @NotNull(message = "Indica si el paciente queda activo o de baja.")
        Boolean activo) {
}
