package sv.clinica.api.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import sv.clinica.api.dto.ErrorResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Punto único de errores: cualquier excepción se convierte en el mismo JSON
 * { timestamp, status, error, message, path, errores }. Nunca devuelve trazas ni detalles internos:
 * esos solo van al log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DATOS_INVALIDOS = "Revisa los datos enviados.";
    public static final String SIN_PERMISO = "No tienes permiso para realizar esta acción.";

    private final RespuestasDeError errores;

    public GlobalExceptionHandler(RespuestasDeError errores) {
        this.errores = errores;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> porCampo = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String campo = snakeCase(error.getField());
            // Si falta el dato, ese es el mensaje útil (antes que formato o longitud)
            if (esObligatorio(error) || !porCampo.containsKey(campo)) {
                porCampo.put(campo, error.getDefaultMessage());
            }
        }
        return errores.respuesta(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, request, porCampo);
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(DatosInvalidosException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, request, Map.of(ex.getCampo(), ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, request,
                Map.of(ex.getParameterName(), "Este dato es obligatorio."));
    }

    /** Falta una cabecera obligatoria o un parámetro no tiene el formato esperado. */
    @ExceptionHandler({ServletRequestBindingException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> peticionIncompleta(Exception ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, "La petición no es válida.", request);
    }

    /** JSON mal formado o valores con formato incorrecto (fecha "31-02", hora "25:00"…). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String campo = mismatch.getPath().get(mismatch.getPath().size() - 1).getFieldName();
            if (campo != null) {
                return errores.respuesta(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, request,
                        Map.of(campo, "El valor no tiene un formato válido."));
            }
        }
        return errores.respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido.", request);
    }

    /** Por ejemplo GET /api/tratamientos/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoIncorrecto(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' no es válido.", request);
    }

    /** El horario ya no está libre (doble reserva evitada). */
    @ExceptionHandler(HorarioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> horarioOcupado(HorarioNoDisponibleException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(CitaNoModificableException.class)
    public ResponseEntity<ErrorResponse> citaNoModificable(CitaNoModificableException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException ex, HttpServletRequest request) {
        Map<String, String> porCampo = ex.getCampo() == null ? null : Map.of(ex.getCampo(), ex.getMessage());
        return errores.respuesta(HttpStatus.CONFLICT, ex.getMessage(), request, porCampo);
    }

    /** Dos personas guardaron cambios sobre el mismo registro a la vez: el segundo cambio no pisa al primero. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> cambioSimultaneo(ObjectOptimisticLockingFailureException ex,
                                                          HttpServletRequest request) {
        return errores.respuesta(HttpStatus.CONFLICT,
                "Otra persona ha cambiado estos datos a la vez. Vuelve a cargarlos e inténtalo de nuevo.", request);
    }

    @ExceptionHandler(CredencialesIncorrectasException.class)
    public ResponseEntity<ErrorResponse> credenciales(CredencialesIncorrectasException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler({SesionNoValidaException.class, MissingRequestCookieException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> sesionNoValida(Exception ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.UNAUTHORIZED, SesionNoValidaException.MENSAJE, request);
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ErrorResponse> bloqueada(CuentaBloqueadaException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.LOCKED, ex.getMessage(), request);
    }

    /** Falta un permiso (@PreAuthorize o comprobación en el servicio). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> sinPermiso(AccessDeniedException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.FORBIDDEN, SIN_PERMISO, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        return errores.respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoContenido(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return errores.respuesta(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Envía los datos como JSON (Content-Type: application/json).", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado", ex);
        return errores.respuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde.", request);
    }

    private static boolean esObligatorio(FieldError error) {
        return "NotBlank".equals(error.getCode()) || "NotNull".equals(error.getCode());
    }

    /** tratamientoId → tratamiento_id, para que coincida con los nombres del JSON. */
    private static String snakeCase(String campo) {
        return campo.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
