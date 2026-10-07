package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import sv.clinica.api.entity.Sexo;
import sv.clinica.api.entity.TipoDocumento;

import java.time.LocalDate;

/**
 * Alta o edición de un paciente: solo datos personales, de contacto y administrativos.
 * Los campos vacíos se guardan vacíos. El documento y la fecha de nacimiento los revisa además PacienteService.
 */
public record PacienteRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre puede tener como máximo 100 caracteres.")
        @Pattern(regexp = ValidacionPacientes.NOMBRE, message = ValidacionPacientes.NOMBRE_MENSAJE)
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios.")
        @Size(max = 100, message = "Los apellidos pueden tener como máximo 100 caracteres.")
        @Pattern(regexp = ValidacionPacientes.NOMBRE, message = ValidacionPacientes.NOMBRE_MENSAJE)
        String apellidos,

        @Schema(description = "Obligatorio si se indica el número de documento")
        TipoDocumento tipoDocumento,

        @Schema(description = "DNI y NIE se comprueban con su letra de control")
        @Size(max = 30, message = "El número de documento es demasiado largo.")
        String numeroDocumento,

        LocalDate fechaNacimiento,

        Sexo sexo,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 25, message = "El teléfono es demasiado largo.")
        @Pattern(regexp = ValidacionPacientes.TELEFONO, message = ValidacionPacientes.TELEFONO_MENSAJE)
        String telefono,

        @Size(max = 150, message = "El correo puede tener como máximo 150 caracteres.")
        @Email(regexp = ValidacionPacientes.EMAIL_OPCIONAL, message = "Escribe un correo válido, por ejemplo nombre@dominio.com.")
        String email,

        @Size(max = 255, message = "La dirección puede tener como máximo 255 caracteres.")
        String direccion,

        @Schema(description = "Nombre y, si se quiere, la relación entre paréntesis: Ana Pérez (madre)")
        @Size(max = 100, message = "El nombre puede tener como máximo 100 caracteres.")
        @Pattern(regexp = ValidacionPacientes.CONTACTO_OPCIONAL, message = ValidacionPacientes.CONTACTO_MENSAJE)
        String contactoEmergenciaNombre,

        @Size(max = 25, message = "El teléfono es demasiado largo.")
        @Pattern(regexp = ValidacionPacientes.TELEFONO_OPCIONAL, message = ValidacionPacientes.TELEFONO_MENSAJE)
        String contactoEmergenciaTelefono,

        @Schema(description = "Solo datos administrativos; la información clínica va en el expediente")
        @Size(max = 1000, message = "Las observaciones pueden tener como máximo 1000 caracteres.")
        String observaciones) {
}
