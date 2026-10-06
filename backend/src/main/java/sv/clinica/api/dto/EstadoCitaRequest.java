package sv.clinica.api.dto;

import jakarta.validation.constraints.NotNull;
import sv.clinica.api.entity.EstadoCita;

/** Confirmar, en consulta, completada o no asistió. Para cancelar o reprogramar hay endpoints propios. */
public record EstadoCitaRequest(
        @NotNull(message = "Indica el nuevo estado.")
        EstadoCita estado) {
}
