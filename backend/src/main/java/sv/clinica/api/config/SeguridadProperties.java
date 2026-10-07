package sv.clinica.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Reglas de la seguridad del software (app.seguridad.* en application.properties).
 *
 * @param jwtSecret        clave con la que se firman los tokens (variable JWT_SECRET, al menos 32 caracteres)
 * @param claveTemporal    solo en desarrollo y pruebas: sin JWT_SECRET se usa una clave aleatoria en cada arranque
 * @param minutosToken     duración del token de acceso
 * @param horasSesion      duración máxima de una sesión; después hay que volver a entrar
 * @param intentosMaximos  contraseñas incorrectas seguidas antes de bloquear la cuenta
 * @param minutosBloqueo   duración del bloqueo
 * @param costeBcrypt      coste de BCrypt al cifrar contraseñas (cada punto más duplica el tiempo; 12 ≈ un cuarto de segundo)
 * @param cookieSegura     la cookie de sesión solo viaja por HTTPS (false solo en tu equipo, que usa http)
 */
@ConfigurationProperties("app.seguridad")
public record SeguridadProperties(
        String jwtSecret,
        @DefaultValue("false") boolean claveTemporal,
        @DefaultValue("15") int minutosToken,
        @DefaultValue("12") int horasSesion,
        @DefaultValue("5") int intentosMaximos,
        @DefaultValue("15") int minutosBloqueo,
        @DefaultValue("12") int costeBcrypt,
        @DefaultValue("true") boolean cookieSegura) {
}
