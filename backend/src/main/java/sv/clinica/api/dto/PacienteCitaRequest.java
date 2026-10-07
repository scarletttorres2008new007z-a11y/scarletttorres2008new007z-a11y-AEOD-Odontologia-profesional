package sv.clinica.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Ficha del paciente a la que se vincula una cita (por ejemplo, una reservada en la web). */
public record PacienteCitaRequest(
        @NotNull(message = "Elige el paciente.")
        @Positive(message = "El paciente no es válido.")
        Long pacienteId) {
}
