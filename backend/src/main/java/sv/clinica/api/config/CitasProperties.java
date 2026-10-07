package sv.clinica.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Reglas de las citas (app.citas.* en application.properties).
 *
 * @param confirmacionAutomatica         true = las citas de la web entran CONFIRMADAS; false = PENDIENTES
 * @param cancelacionAntelacionHoras     el paciente solo puede cancelar o reprogramar con al menos estas horas
 */
@ConfigurationProperties("app.citas")
public record CitasProperties(
        @DefaultValue("false") boolean confirmacionAutomatica,
        @DefaultValue("4") int cancelacionAntelacionHoras) {
}
