package sv.clinica.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.repository.RolRepository;
import sv.clinica.api.repository.TratamientoRepository;
import sv.clinica.api.repository.UsuarioRepository;
import sv.clinica.api.security.LimitadorDePeticiones;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de integración: la base de pruebas (MySQL o MariaDB) recién creada con las migraciones,
 * los datos de ejemplo y un reloj fijo en el lunes 12/10/2026 a las 08:00 (hora de Madrid).
 * Cada test crea los usuarios y pacientes que necesita; al terminar se borran (el administrador inicial se conserva).
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
    static final LocalDateTime AHORA = LUNES.atTime(8, 0);

    /** Contraseña de los usuarios que crean los tests. */
    static final String CLAVE = "Clave-de-prueba-2026";
    /** Lo crea AdministradorInicial al arrancar, con una contraseña al azar que los tests no conocen. */
    static final String ADMIN_INICIAL = "admin";

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(ZonedDateTime.of(AHORA, MADRID).toInstant(), MADRID);
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
    ObjectMapper json;
    @Autowired
    TratamientoRepository tratamientos;
    @Autowired
    OdontologoRepository odontologos;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    RolRepository roles;
    @Autowired
    PasswordEncoder passwords;
    @Autowired
    LimitadorDePeticiones limitador;

    /** Permisos de cada rol tal y como los dejan las migraciones; se restauran al terminar cada test. */
    private static List<Object[]> permisosIniciales;

    /**
     * Configuración de la agenda de los datos de ejemplo (odontólogos, tratamientos, horarios y bloqueos). Los tests
     * de configuración la cambian; al terminar cada test se deja como estaba.
     */
    private static final List<String> CONFIGURACION = List.of("odontologos", "tratamientos", "odontologo_tratamientos",
            "horarios_clinica", "horarios_odontologo", "bloqueos");
    private static Map<String, List<Map<String, Object>>> configuracionInicial;

    @BeforeEach
    void contadoresACero() {
        limitador.vaciar();
        if (permisosIniciales == null) {
            permisosIniciales = jdbc.query("select rol_id, permiso_id from rol_permisos",
                    (fila, n) -> new Object[]{fila.getLong("rol_id"), fila.getLong("permiso_id")});
        }
        if (configuracionInicial == null) {
            Map<String, List<Map<String, Object>>> copia = new LinkedHashMap<>();
            for (String tabla : CONFIGURACION) copia.put(tabla, jdbc.queryForList("select * from " + tabla));
            configuracionInicial = copia;
        }
    }

    /**
     * Cada test empieza con la agenda vacía, sin pacientes ni usuarios propios y con los permisos de cada rol como
     * los dejan las migraciones (los datos de ejemplo se conservan).
     */
    @AfterEach
    void vaciarAgenda() {
        jdbc.update("delete from notificaciones");
        jdbc.update("delete from lista_espera");
        jdbc.update("delete from agenda_ocupacion");
        jdbc.update("update citas set cita_anterior_id = null");
        jdbc.update("delete from citas");
        jdbc.update("delete from pacientes");
        jdbc.update("update odontologos set usuario_id = null");
        jdbc.update("delete from solicitudes_contacto");
        jdbc.update("delete from auditoria");
        jdbc.update("delete from sesiones");
        jdbc.update("delete from usuario_roles where usuario_id in (select id from usuarios where username <> ?)", ADMIN_INICIAL);
        jdbc.update("delete from usuarios where username <> ?", ADMIN_INICIAL);
        jdbc.update("delete from rol_permisos");
        jdbc.batchUpdate("insert into rol_permisos (rol_id, permiso_id) values (?, ?)", permisosIniciales);
        restaurarConfiguracion();
        limitador.vaciar();
    }

    /** Deja odontólogos, tratamientos, horarios y bloqueos como en los datos de ejemplo (las citas ya están borradas). */
    private void restaurarConfiguracion() {
        for (String tabla : List.of("bloqueos", "horarios_odontologo", "horarios_clinica", "odontologo_tratamientos")) {
            jdbc.update("delete from " + tabla);
        }
        for (String tabla : List.of("odontologos", "tratamientos")) {
            List<Map<String, Object>> filas = configuracionInicial.get(tabla);
            String ids = filas.stream().map(fila -> fila.get("id").toString()).collect(Collectors.joining(","));
            jdbc.update("delete from " + tabla + " where id not in (" + ids + ")");
            for (Map<String, Object> fila : filas) {
                List<String> columnas = fila.keySet().stream().filter(c -> !c.equals("id")).toList();
                List<Object> valores = new ArrayList<>(columnas.stream().map(fila::get).toList());
                valores.add(fila.get("id"));
                jdbc.update("update " + tabla + " set " + String.join(" = ?, ", columnas) + " = ? where id = ?",
                        valores.toArray());
            }
        }
        for (String tabla : List.of("odontologo_tratamientos", "horarios_clinica", "horarios_odontologo", "bloqueos")) {
            List<Map<String, Object>> filas = configuracionInicial.get(tabla);
            if (filas.isEmpty()) continue;
            List<String> columnas = List.copyOf(filas.get(0).keySet());
            jdbc.batchUpdate("insert into " + tabla + " (" + String.join(", ", columnas) + ") values ("
                            + String.join(", ", columnas.stream().map(c -> "?").toList()) + ")",
                    filas.stream().map(fila -> columnas.stream().map(fila::get).toArray()).toList());
        }
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

    /* ─────────────── Usuarios y sesiones ─────────────── */

    /** Crea un usuario activo con la contraseña {@link #CLAVE} y esos roles. */
    Usuario crearUsuario(String username, String... codigosDeRol) {
        Usuario usuario = new Usuario(username, username + "@example.com", "Prueba " + username,
                passwords.encode(CLAVE), AHORA.minusDays(1));
        usuario.cambiarRoles(roles.findByCodigoIn(Arrays.asList(codigosDeRol)), AHORA.minusDays(1));
        return usuarios.save(usuario);
    }

    /** Da permisos a un rol directamente en la base (como haría un administrador desde el software). */
    void darPermisos(String rol, String... permisos) {
        for (String permiso : permisos) {
            jdbc.update("insert into rol_permisos (rol_id, permiso_id) select r.id, p.id from roles r, permisos p "
                    + "where r.codigo = ? and p.codigo = ?", rol, permiso);
        }
    }

    MockHttpServletRequestBuilder login(String usuario, String password) throws Exception {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("usuario", usuario, "password", password)));
    }

    /** Inicia sesión con {@link #CLAVE} y devuelve el token de acceso. */
    String entrar(String username) throws Exception {
        String cuerpo = mvc.perform(login(username, CLAVE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("token_acceso").asText();
    }

    /** Añade "Authorization: Bearer …" a la petición. */
    static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder peticion, Object cuerpo) throws Exception {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }

    JsonNode leer(String cuerpo) throws Exception {
        return json.readTree(cuerpo);
    }
}
