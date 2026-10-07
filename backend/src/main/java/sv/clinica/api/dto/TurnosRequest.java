package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Todos los turnos de la semana de un odontólogo; sustituyen a los que tenía. */
public record TurnosRequest(
        @NotNull(message = "Envía los turnos.")
        @Schema(description = "Puede haber varios tramos el mismo día; un día sin tramos no trabaja")
        List<TurnoSemanal> turnos) {
}
