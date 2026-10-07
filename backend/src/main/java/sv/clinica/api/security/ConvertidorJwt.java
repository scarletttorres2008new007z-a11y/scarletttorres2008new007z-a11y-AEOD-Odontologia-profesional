package sv.clinica.api.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import sv.clinica.api.exception.SesionNoValidaException;
import sv.clinica.api.service.AuthService;

/**
 * Convierte un token con firma válida en el usuario que hace la petición. Además de la firma y la caducidad
 * (ya comprobadas), exige que su sesión siga abierta y que el usuario siga activo.
 */
@Component
public class ConvertidorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthService auth;

    public ConvertidorJwt(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long usuarioId = parsear(jwt.getSubject());
        Object sesion = jwt.getClaim(TokenService.CLAIM_SESION);
        Long sesionId = sesion instanceof Number numero ? numero.longValue() : null;
        if (usuarioId == null || sesionId == null) {
            throw new InvalidBearerTokenException(SesionNoValidaException.MENSAJE);
        }
        return auth.validarSesion(sesionId, usuarioId)
                .map(usuario -> new AutenticacionUsuario(usuario, jwt))
                .orElseThrow(() -> new InvalidBearerTokenException(SesionNoValidaException.MENSAJE));
    }

    private static Long parsear(String valor) {
        try {
            return valor == null ? null : Long.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
