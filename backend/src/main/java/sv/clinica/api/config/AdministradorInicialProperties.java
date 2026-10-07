package sv.clinica.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Primer administrador del software (app.admin-inicial.*), sin contraseñas en el código.
 *
 * @param username         usuario (variable ADMIN_USERNAME, por defecto "admin")
 * @param email            correo (variable ADMIN_EMAIL)
 * @param password         contraseña (variable ADMIN_PASSWORD). Si está definida, se aplica al arrancar; quítala después
 * @param generarPassword  solo en desarrollo: si no hay ningún administrador, crea uno con una contraseña aleatoria
 *                         y la muestra una vez en la consola
 */
@ConfigurationProperties("app.admin-inicial")
public record AdministradorInicialProperties(
        @DefaultValue("admin") String username,
        String email,
        String password,
        @DefaultValue("false") boolean generarPassword) {
}
