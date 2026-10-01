package sv.clinica.landing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Respuesta JSON sencilla: { "success": true, "message": "..." }.
 * En errores de validación incluye "errores" con un mensaje por campo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(boolean success, String message, Map<String, String> errores) {

    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message, null);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message, null);
    }

    public static ApiResponse error(String message, Map<String, String> errores) {
        return new ApiResponse(false, message, errores);
    }
}
