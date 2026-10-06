package sv.clinica.api.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.ErrorResponse;

/**
 * Errores que ocurren fuera de los controllers (por ejemplo, una petición que el servidor rechaza antes de llegar
 * a la API). Sustituye la respuesta estándar de Spring para que también tengan el formato único de la API.
 */
@Hidden
@RestController
public class ControladorDeErrores implements ErrorController {

    private final RespuestasDeError errores;

    public ControladorDeErrores(RespuestasDeError errores) {
        this.errores = errores;
    }

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        Object codigo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = codigo instanceof Integer valor && HttpStatus.resolve(valor) != null
                ? HttpStatus.valueOf(valor) : HttpStatus.INTERNAL_SERVER_ERROR;
        Object ruta = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return errores.respuesta(status, mensaje(status), ruta != null ? ruta.toString() : request.getRequestURI(), null);
    }

    private static String mensaje(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "La petición no es válida.";
            case UNAUTHORIZED -> "Inicia sesión para continuar.";
            case FORBIDDEN -> GlobalExceptionHandler.SIN_PERMISO;
            case NOT_FOUND -> "La ruta solicitada no existe.";
            case METHOD_NOT_ALLOWED -> "Método no permitido.";
            default -> "Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde.";
        };
    }
}
