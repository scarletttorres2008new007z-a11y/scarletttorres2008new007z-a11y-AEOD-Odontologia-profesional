package sv.clinica.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Reserva de un horario concreto que el paciente eligió entre los que devolvió /api/disponibilidad.
 * El backend vuelve a comprobar que sigue libre antes de guardarla.
 */
public record CitaRequest(

        @NotNull(message = "Selecciona un tratamiento.")
        @Positive(message = "El tratamiento no es válido.")
        Long tratamientoId,

        @NotNull(message = "Selecciona un horario.")
        @Positive(message = "El odontólogo no es válido.")
        Long odontologoId,

        @NotNull(message = "Selecciona un horario.")
        LocalDate fecha,

        @NotNull(message = "Selecciona un horario.")
        LocalTime horaInicio,

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

        @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres.")
        String mensaje) {
}
