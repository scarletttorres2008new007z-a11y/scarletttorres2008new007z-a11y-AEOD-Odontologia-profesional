package sv.clinica.landing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.LocalTime;

/**
 * Reglas de la agenda (app.agenda.* en application.properties).
 *
 * @param intervaloMinutos           cada cuántos minutos se ofrece una hora de inicio (múltiplo de 15)
 * @param antelacionMinimaMinutos    margen mínimo entre "ahora" y la cita para reservar online
 * @param diasReservaMaximos         hasta cuántos días vista se puede reservar
 * @param diasBusquedaAlternativas   cuántos días se exploran al buscar las próximas opciones
 * @param inicioTarde                hora a partir de la cual un horario cuenta como "tarde"
 */
@ConfigurationProperties("app.agenda")
public record AgendaProperties(
        @DefaultValue("30") int intervaloMinutos,
        @DefaultValue("60") int antelacionMinimaMinutos,
        @DefaultValue("180") int diasReservaMaximos,
        @DefaultValue("30") int diasBusquedaAlternativas,
        @DefaultValue("14:00") LocalTime inicioTarde) {
}
