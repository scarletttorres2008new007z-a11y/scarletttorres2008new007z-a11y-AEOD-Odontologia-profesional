package sv.clinica.landing.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Antes de arrancar, comprueba que llegan las variables de entorno obligatorias.
 * En test y prod no tienen valor por defecto: si falta alguna, el backend se detiene y dice cuál,
 * en lugar de fallar más tarde con un error de conexión difícil de entender.
 * En dev tienen valores por defecto para XAMPP (application-dev.properties).
 */
public class VariablesDeEntornoObligatorias implements EnvironmentPostProcessor, Ordered {

    /** Propiedad de configuración → variable de entorno que la rellena. */
    private static final Map<String, String> OBLIGATORIAS = new LinkedHashMap<>();

    static {
        OBLIGATORIAS.put("spring.datasource.url", "DB_URL");
        OBLIGATORIAS.put("spring.datasource.username", "DB_USERNAME");
        OBLIGATORIAS.put("spring.datasource.password", "DB_PASSWORD");
        OBLIGATORIAS.put("app.cors.origenes", "CORS_ALLOWED_ORIGINS");
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> faltan = new ArrayList<>();
        OBLIGATORIAS.forEach((propiedad, variable) -> {
            try {
                environment.getProperty(propiedad);
            } catch (IllegalArgumentException sinValor) {
                faltan.add(variable);
            }
        });
        if (!faltan.isEmpty()) {
            throw new IllegalStateException("Faltan variables de entorno: " + String.join(", ", faltan)
                    + ". Defínelas antes de arrancar (ver backend/README.md, apartado «Entornos»).");
        }
    }

    /** Después de leer los archivos application*.properties. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
