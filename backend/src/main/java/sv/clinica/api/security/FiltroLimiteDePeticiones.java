package sv.clinica.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import sv.clinica.api.exception.RespuestasDeError;

import java.io.IOException;

/**
 * Límite de peticiones por IP en los endpoints que se usan sin sesión (login, reserva de cita y contacto)
 * y en el cambio de la propia contraseña.
 * Al superarlo responde 429 con la cabecera Retry-After (segundos de espera).
 * Va dentro de la cadena de Spring Security, antes de comprobar ningún token.
 */
public class FiltroLimiteDePeticiones extends OncePerRequestFilter {

    private static final String MENSAJE = "Has hecho demasiadas solicitudes seguidas. Espera unos minutos y vuelve a intentarlo.";

    private final LimitadorDePeticiones limitador;
    private final RespuestasDeError errores;

    public FiltroLimiteDePeticiones(LimitadorDePeticiones limitador, RespuestasDeError errores) {
        this.limitador = limitador;
        this.errores = errores;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitadorDePeticiones.Tipo tipo = tipoDe(request);
        if (tipo != null) {
            long esperar = limitador.consumir(tipo, request.getRemoteAddr());
            if (esperar > 0) {
                response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(esperar));
                errores.escribir(request, response, HttpStatus.TOO_MANY_REQUESTS, MENSAJE);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static LimitadorDePeticiones.Tipo tipoDe(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        // Cambiar la propia contraseña también prueba contraseñas: cuenta como un intento de entrar
        if (HttpMethod.PUT.matches(request.getMethod())) {
            return "/api/auth/password".equals(ruta) ? LimitadorDePeticiones.Tipo.LOGIN : null;
        }
        if (!HttpMethod.POST.matches(request.getMethod())) return null;
        return switch (ruta) {
            case "/api/auth/login" -> LimitadorDePeticiones.Tipo.LOGIN;
            // Las reservas del personal (con sesión) no cuentan: el límite es para la web pública
            case "/api/citas" -> request.getHeader(HttpHeaders.AUTHORIZATION) == null ? LimitadorDePeticiones.Tipo.RESERVA : null;
            // Cancelar desde la web cuenta como una reserva más: así nadie puede probar códigos sin límite
            case "/api/citas/cancelacion" -> LimitadorDePeticiones.Tipo.RESERVA;
            case "/api/contacto" -> LimitadorDePeticiones.Tipo.CONTACTO;
            default -> null;
        };
    }
}
