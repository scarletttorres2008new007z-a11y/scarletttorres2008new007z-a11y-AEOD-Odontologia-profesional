package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Alta o cambio de un tratamiento. La duración es la que usa la agenda para calcular los huecos libres. */
public record TratamientoConfigRequest(

        @NotBlank(message = "Escribe el nombre.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String nombre,

        @Size(max = 255, message = "La descripción corta no puede superar los 255 caracteres.")
        String descripcionCorta,

        @DecimalMin(value = "0", message = "El precio no puede ser negativo.")
        @DecimalMax(value = "99999.99", message = "El precio es demasiado alto.")
        @Digits(integer = 5, fraction = 2, message = "El precio admite como mucho dos decimales.")
        @Schema(description = "Precio orientativo «desde», en euros. 0 o vacío = la web lo muestra como gratuito")
        BigDecimal precioDesde,

        @Size(max = 80, message = "El texto de la duración no puede superar los 80 caracteres.")
        @Schema(description = "Duración orientativa en texto, por ejemplo «2 o 3 sesiones». Las citas usan duracion_minutos", example = "30–45 min")
        String duracionAproximada,

        @NotNull(message = "Indica cuántos minutos dura la cita.")
        @Min(value = 15, message = "La cita tiene que durar al menos 15 minutos.")
        @Max(value = 480, message = "La cita no puede durar más de 8 horas.")
        @Schema(example = "60")
        Integer duracionMinutos,

        @Schema(description = "Quién lo hace; vacío = cualquier odontólogo")
        List<Long> odontologoIds) {
}
