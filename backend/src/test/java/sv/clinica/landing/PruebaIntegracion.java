package sv.clinica.landing;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import sv.clinica.landing.entity.Odontologo;
import sv.clinica.landing.entity.Tratamiento;
import sv.clinica.landing.repository.OdontologoRepository;
import sv.clinica.landing.repository.TratamientoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Base de los tests de integración: H2 en memoria con los datos de ejemplo
 * y un reloj fijo en el lunes 12/10/2026 a las 08:00 (hora de Madrid).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PruebaIntegracion.RelojFijo.class)
abstract class PruebaIntegracion {

    static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    static final LocalDate MARTES = LUNES.plusDays(1);
    static final LocalDate MIERCOLES = LUNES.plusDays(2);
    static final LocalDate DOMINGO = LUNES.plusDays(6);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(ZonedDateTime.of(LUNES.atTime(8, 0), MADRID).toInstant(), MADRID);
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TratamientoRepository tratamientos;
    @Autowired
    OdontologoRepository odontologos;

    /** Cada test empieza con la agenda vacía (los datos de ejemplo se conservan). */
    @AfterEach
    void vaciarAgenda() {
        jdbc.update("delete from notificaciones");
        jdbc.update("delete from lista_espera");
        jdbc.update("delete from agenda_ocupacion");
        jdbc.update("update citas set cita_anterior_id = null");
        jdbc.update("delete from citas");
        jdbc.update("delete from solicitudes_contacto");
    }

    Tratamiento tratamiento(String nombre) {
        return tratamientos.findByNombre(nombre).orElseThrow();
    }

    Odontologo odontologo(String nombre) {
        return odontologos.findByNombre(nombre).orElseThrow();
    }

    String citaJson(Tratamiento t, Odontologo o, LocalDate fecha, String hora) {
        return """
                {"tratamiento_id":%d,"odontologo_id":%d,"fecha":"%s","hora_inicio":"%s",
                 "nombre":"Lucía Gómez","telefono":"600 123 456","email":"lucia@example.com"}
                """.formatted(t.getId(), o.getId(), fecha, hora);
    }
}
