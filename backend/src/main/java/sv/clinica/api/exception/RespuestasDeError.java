package sv.clinica.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import sv.clinica.api.dto.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Construye los errores con el formato único de la API. Lo usan el manejador global de excepciones
 * y las piezas que responden antes de llegar a los controllers (seguridad y límite de peticiones).
 */
@Component
public class RespuestasDeError {

    private final Clock clock;
    private final ObjectMapper json;

    public RespuestasDeError(Clock clock, ObjectMapper json) {
        this.clock = clock;
        this.json = json;
    }

    public ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String message, HttpServletRequest request) {
        return respuesta(status, message, request, null);
    }

    public ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String message, HttpServletRequest request,
                                                   Map<String, String> errores) {
        return respuesta(status, message, request.getRequestURI(), errores);
    }

    public ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String message, String path,
                                                   Map<String, String> errores) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.de(status, message, path, errores, OffsetDateTime.now(clock)));
    }

    /** Escribe el error directamente en la respuesta (filtros y seguridad, donde no hay controller). */
    public void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getOutputStream(),
                ErrorResponse.de(status, message, request.getRequestURI(), null, OffsetDateTime.now(clock)));
    }
}
