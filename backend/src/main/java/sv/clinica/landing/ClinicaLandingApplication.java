package sv.clinica.landing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
public class ClinicaLandingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaLandingApplication.class, args);
    }

    /** Reloj en la zona horaria de la clínica: decide qué es "hoy" al validar fechas. */
    @Bean
    Clock clock(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
