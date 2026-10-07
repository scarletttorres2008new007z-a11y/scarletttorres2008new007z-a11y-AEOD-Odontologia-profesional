package sv.clinica.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Peticiones permitidas por IP en los endpoints que se pueden usar sin sesión (app.limites.*).
 * Frenan los ataques de fuerza bruta al login y el spam de reservas o mensajes desde la web.
 *
 * @param loginPorMinuto    intentos de inicio de sesión por minuto
 * @param reservasPorHora   reservas de cita por hora
 * @param contactosPorHora  mensajes del formulario de contacto por hora
 */
@ConfigurationProperties("app.limites")
public record LimitesProperties(
        @DefaultValue("10") int loginPorMinuto,
        @DefaultValue("20") int reservasPorHora,
        @DefaultValue("20") int contactosPorHora) {
}
