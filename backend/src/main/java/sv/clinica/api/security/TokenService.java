package sv.clinica.api.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import sv.clinica.api.config.SeguridadProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Emite los tokens de acceso (JWT firmado con HS256 y JWT_SECRET). Son cortos (15 min por defecto) y solo dicen
 * quién es el usuario y a qué sesión pertenecen; los permisos se leen de la base de datos en cada petición.
 */
@Service
public class TokenService {

    public static final String EMISOR = "aeod-api";
    public static final String CLAIM_SESION = "sid";

    private final JwtEncoder encoder;
    private final Duration duracion;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, SeguridadProperties seguridad, Clock clock) {
        this.encoder = encoder;
        this.duracion = Duration.ofMinutes(seguridad.minutosToken());
        this.clock = clock;
    }

    public String emitir(Long usuarioId, Long sesionId) {
        Instant ahora = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuarioId))
                .claim(CLAIM_SESION, sesionId)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    public long segundosDeValidez() {
        return duracion.toSeconds();
    }
}
