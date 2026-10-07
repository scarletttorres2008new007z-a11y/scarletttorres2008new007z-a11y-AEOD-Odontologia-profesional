package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Alta o cambio de un odontólogo. El nombre, la especialidad y la descripción son los que enseña la landing. */
public record OdontologoConfigRequest(

        @NotBlank(message = "Escribe el nombre.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        @Schema(example = "Dra. Ana Villar")
        String nombre,

        @Size(max = 150, message = "La especialidad no puede superar los 150 caracteres.")
        String especialidad,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
        String descripcion,

        @Positive(message = "El usuario no es válido.")
        @Schema(description = "Su usuario del software, para que vea su propia agenda; vacío = sin usuario")
        Long usuarioId) {
}
