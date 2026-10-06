package sv.clinica.api.dto;

import jakarta.validation.constraints.Size;

/** Notas internas de la cita. Vacías = sin notas. */
public record NotasCitaRequest(
        @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres.")
        String notasInternas) {
}
