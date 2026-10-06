package sv.clinica.api.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import sv.clinica.api.config.SeguridadProperties;

import java.time.Duration;

/**
 * Cookie con el token de refresco: HttpOnly (JavaScript no puede leerla), SameSite=Strict (no viaja en peticiones
 * desde otros sitios), Secure en producción (solo HTTPS) y limitada a /api/auth, el único sitio donde se usa.
 */
@Component
public class CookieDeSesion {

    public static final String NOMBRE = "aeod_sesion";
    private static final String RUTA = "/api/auth";

    private final SeguridadProperties seguridad;

    public CookieDeSesion(SeguridadProperties seguridad) {
        this.seguridad = seguridad;
    }

    public String crear(String refreshToken) {
        return base(refreshToken).maxAge(Duration.ofHours(seguridad.horasSesion())).build().toString();
    }

    public String borrar() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(seguridad.cookieSegura())
                .sameSite("Strict")
                .path(RUTA);
    }
}
