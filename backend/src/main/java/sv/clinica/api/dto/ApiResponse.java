package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Respuesta JSON sencilla: { "success": true, "message": "..." }.
 * En errores de validación incluye "errores" con un mensaje por campo,
 * y cuando hay algo que devolver (por ejemplo la cita creada), "datos".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(boolean success, String message, Map<String, String> errores, Object datos) {

    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message, null, null);
    }

    public static ApiResponse ok(String message, Object datos) {
        return new ApiResponse(true, message, null, datos);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message, null, null);
    }

    public static ApiResponse error(String message, Map<String, String> errores) {
        return new ApiResponse(false, message, errores, null);
    }
}
