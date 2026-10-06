package sv.clinica.landing;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
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

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Base de los tests de integración: la base de pruebas (MySQL o MariaDB) recién creada con las migraciones,
 * los datos de ejemplo y un reloj fijo en el lunes 12/10/2026 a las 08:00 (hora de Madrid).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({PruebaIntegracion.RelojFijo.class, PruebaIntegracion.BaseDePruebasLimpia.class})
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

    /**
     * Al arrancar, borra la base de pruebas y la crea de nuevo con las migraciones (así se prueban también las migraciones).
     * Por seguridad, solo borra una base cuyo nombre contenga "prueba" o "test".
     */
    @TestConfiguration
    static class BaseDePruebasLimpia {
        @Bean
        FlywayMigrationStrategy borrarYMigrar() {
            return flyway -> {
                comprobarQueEsBaseDePruebas(flyway);
                flyway.clean();
                flyway.migrate();
            };
        }

        private static void comprobarQueEsBaseDePruebas(Flyway flyway) {
            try (Connection conexion = flyway.getConfiguration().getDataSource().getConnection()) {
                String base = String.valueOf(conexion.getCatalog()).toLowerCase();
                if (!base.contains("prueba") && !base.contains("test")) {
                    throw new IllegalStateException("Las pruebas borran su base de datos y '" + base
                            + "' no parece una base de pruebas. Usa TEST_DB_URL con una base que contenga 'pruebas' en el nombre.");
                }
            } catch (SQLException e) {
                throw new IllegalStateException("No se pudo conectar con la base de pruebas.", e);
            }
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
