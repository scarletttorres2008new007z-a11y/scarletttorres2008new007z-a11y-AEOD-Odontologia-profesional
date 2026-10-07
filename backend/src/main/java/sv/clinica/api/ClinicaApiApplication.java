package sv.clinica.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClinicaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaApiApplication.class, args);
    }

    /** Reloj en la zona horaria de la clínica: decide qué es "hoy" al validar fechas. */
    @Bean
    Clock clock(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
