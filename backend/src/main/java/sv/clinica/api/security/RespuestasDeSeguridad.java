package sv.clinica.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import sv.clinica.api.exception.GlobalExceptionHandler;
import sv.clinica.api.exception.RespuestasDeError;
import sv.clinica.api.exception.SesionNoValidaException;

import java.io.IOException;

/** 401 y 403 de Spring Security con el formato único de errores de la API. */
@Component
public class RespuestasDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final RespuestasDeError errores;

    public RespuestasDeSeguridad(RespuestasDeError errores) {
        this.errores = errores;
    }

    /** Sin sesión (401): no envió token, o el token caducó o su sesión se cerró. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        boolean conToken = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        errores.escribir(request, response, HttpStatus.UNAUTHORIZED,
                conToken ? SesionNoValidaException.MENSAJE : "Inicia sesión para continuar.");
    }

    /** Con sesión pero sin el permiso necesario (403). */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        errores.escribir(request, response, HttpStatus.FORBIDDEN, GlobalExceptionHandler.SIN_PERMISO);
    }
}
