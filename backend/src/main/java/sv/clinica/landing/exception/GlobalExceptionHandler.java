package sv.clinica.landing.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import sv.clinica.landing.dto.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte cualquier error en una respuesta JSON sencilla.
 * Nunca devuelve trazas ni detalles internos: esos solo van al log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DATOS_INVALIDOS = "Revisa los datos enviados.";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String campo = snakeCase(error.getField());
            // Si falta el dato, ese es el mensaje útil (antes que formato o longitud)
            if (esObligatorio(error) || !errores.containsKey(campo)) {
                errores.put(campo, error.getDefaultMessage());
            }
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(DATOS_INVALIDOS, errores));
    }

    /** El horario ya no está libre (doble reserva evitada). */
    @ExceptionHandler(HorarioNoDisponibleException.class)
    public ResponseEntity<ApiResponse> horarioOcupado(HorarioNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CitaNoModificableException.class)
    public ResponseEntity<ApiResponse> citaNoModificable(CitaNoModificableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> parametroFaltante(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(DATOS_INVALIDOS,
                Map.of(ex.getParameterName(), "Este dato es obligatorio.")));
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ApiResponse> datosInvalidos(DatosInvalidosException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(DATOS_INVALIDOS, Map.of(ex.getCampo(), ex.getMessage())));
    }

    /** JSON mal formado o valores con formato incorrecto (fecha "31-02", hora "25:00"…). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> jsonIlegible(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String campo = mismatch.getPath().get(mismatch.getPath().size() - 1).getFieldName();
            if (campo != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(DATOS_INVALIDOS, Map.of(campo, "El valor no tiene un formato válido.")));
            }
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("El cuerpo de la petición no es un JSON válido."));
    }

    /** Por ejemplo GET /api/tratamientos/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("El parámetro '" + ex.getName() + "' no es válido."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> rutaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("La ruta solicitada no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error("Método no permitido."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse> tipoContenido(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("Envía los datos como JSON (Content-Type: application/json)."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> inesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde."));
    }

    private static boolean esObligatorio(FieldError error) {
        return "NotBlank".equals(error.getCode()) || "NotNull".equals(error.getCode());
    }

    /** tratamientoId → tratamiento_id, para que coincida con los nombres del JSON. */
    private static String snakeCase(String campo) {
        return campo.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
