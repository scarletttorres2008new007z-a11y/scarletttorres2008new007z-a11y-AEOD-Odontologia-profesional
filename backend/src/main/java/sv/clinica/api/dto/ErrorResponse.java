package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Formato único de los errores de la API, venga de donde venga (validación, permisos, sesión, conflicto…).
 * La landing y el software leen "message" y, en los errores de datos, "errores" (un mensaje por campo).
 */
@Schema(description = "Error de la API", requiredProperties = {"timestamp", "status", "error", "message", "path"})
public record ErrorResponse(
        @Schema(description = "Momento del error", example = "2026-10-12T10:25:00+02:00")
        OffsetDateTime timestamp,
        @Schema(description = "Código HTTP", example = "409")
        int status,
        @Schema(description = "Nombre del código HTTP", example = "CONFLICT")
        String error,
        @Schema(description = "Mensaje para mostrar a la persona", example = "Este horario acaba de ser reservado. Selecciona otra opción.")
        String message,
        @Schema(description = "Ruta de la petición", example = "/api/citas")
        String path,
        @Schema(description = "Un mensaje por cada campo con datos no válidos", example = "{\"email\": \"Escribe un correo válido.\"}")
        Map<String, String> errores) {

    public static ErrorResponse de(HttpStatus status, String message, String path, Map<String, String> errores,
                                   OffsetDateTime ahora) {
        return new ErrorResponse(ahora, status.value(), status.name(), message, path, errores);
    }
}
