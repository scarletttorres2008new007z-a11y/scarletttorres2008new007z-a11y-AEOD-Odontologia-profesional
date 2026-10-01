package sv.clinica.landing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para la landing en desarrollo. Los orígenes se definen en
 * app.cors.origenes (application.properties); nunca se usa "*".
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origenes;

    public CorsConfig(@Value("${app.cors.origenes}") String[] origenes) {
        this.origenes = origenes;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenes)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                .maxAge(3600);
    }
}
