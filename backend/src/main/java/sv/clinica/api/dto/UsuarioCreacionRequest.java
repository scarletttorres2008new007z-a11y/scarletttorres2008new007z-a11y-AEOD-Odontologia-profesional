package sv.clinica.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UsuarioCreacionRequest(
        @NotBlank(message = "El usuario es obligatorio.")
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres.")
        @Pattern(regexp = ValidacionUsuarios.USERNAME, message = ValidacionUsuarios.USERNAME_MENSAJE)
        String username,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "Escribe un correo válido.")
        @Size(max = 150, message = "El correo es demasiado largo.")
        String email,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre puede tener como máximo 100 caracteres.")
        String nombre,

        @NotBlank(message = "La contraseña es obligatoria.")
        String password,

        /** Códigos de rol (ADMINISTRADOR, RECEPCION…). Asignarlos exige el permiso usuarios.asignar_roles. */
        List<String> roles) {
}
