package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** El horario completo de la semana. Los días que no se envían quedan cerrados. */
public record HorarioClinicaRequest(
        @NotNull(message = "Envía los días en que abre la clínica.")
        @Schema(description = "Días en que abre; el que no aparece queda cerrado")
        List<DiaDeApertura> dias) {
}
