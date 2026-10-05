package sv.clinica.landing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactoRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
        @Pattern(regexp = ValidacionPatrones.NOMBRE, message = "El nombre solo puede contener letras, espacios y guiones.")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres.")
        @Pattern(regexp = ValidacionPatrones.TELEFONO, message = "El teléfono debe tener entre 9 y 15 dígitos (se admite el prefijo +).")
        String telefono,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres.")
        @Email(regexp = ValidacionPatrones.EMAIL, message = "El correo electrónico no es válido.")
        String email,

        @Size(max = 120, message = "El tratamiento no puede superar los 120 caracteres.")
        String tratamiento,

        @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres.")
        String mensaje) {
}
