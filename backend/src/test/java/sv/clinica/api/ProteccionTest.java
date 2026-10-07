package sv.clinica.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Límite de peticiones, cabeceras de seguridad, CORS, rutas privadas, auditoría de citas y Swagger. */
class ProteccionTest extends PruebaIntegracion {

    @Test
    void demasiadosIntentosDeEntrarDesdeLaMismaIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(login("nadie", "incorrecta").with(desdeIp("203.0.113.7"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("nadie", "incorrecta").with(desdeIp("203.0.113.7")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value(
                        "Has hecho demasiadas solicitudes seguidas. Espera unos minutos y vuelve a intentarlo."));
        // Otra IP no se ve afectada
        mvc.perform(login("nadie", "incorrecta").with(desdeIp("198.51.100.20"))).andExpect(status().isUnauthorized());
    }

    @Test
    void laWebNoPuedeEnviarMensajesSinLimite() throws Exception {
        String mensaje = """
                {"nombre":"Lucía Gómez","telefono":"+34 600 123 456","email":"lucia@example.com"}""";
        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(mensaje))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(mensaje))
                .andExpect(status().isTooManyRequests());
        assertThat(jdbc.queryForObject("select count(*) from solicitudes_contacto", Integer.class)).isEqualTo(20);
    }

    @Test
    void cabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
        // Por HTTPS, además, HSTS
        mvc.perform(get("/api/health").secure(true))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=")));
    }

    @Test
    void laLandingSoloPuedeLlamarALoPublico() throws Exception {
        mvc.perform(options("/api/disponibilidad").header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"));
        // Las rutas del software no aceptan llamadas desde otra web, ni siquiera desde la landing
        mvc.perform(options("/api/usuarios").header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void loQueNoEsPublicoPideSesion() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auditoria")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ruta-que-no-existe")).andExpect(status().isUnauthorized());
        // Con sesión, una ruta inexistente es un 404 con el formato de siempre
        crearUsuario("marta", "RECEPCION");
        mvc.perform(conToken(get("/api/ruta-que-no-existe"), entrar("marta")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void lasReservasDeLaWebQuedanEnLaAuditoria() throws Exception {
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(tratamiento("Limpieza dental"), odontologo("Dra. Ana Villar"), MARTES, "10:00")))
                .andExpect(status().isCreated());

        var registro = jdbc.queryForMap("select usuario_id, origen, accion, entidad, valor_nuevo from auditoria");
        assertThat(registro.get("usuario_id")).isNull();
        assertThat(registro).containsEntry("origen", "LANDING").containsEntry("accion", "RESERVAR").containsEntry("entidad", "CITA");
        assertThat((String) registro.get("valor_nuevo"))
                .contains("\"estado\":\"PENDIENTE\"", "\"hora_inicio\":\"10:00\"")
                .doesNotContain("Lucía", "lucia@example.com", "600");
    }

    @Test
    void documentacionDeLaApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.SesionResponse.properties.token_acceso").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.message").exists())
                .andExpect(jsonPath("$.paths['/api/usuarios'].get.responses.default").exists());
    }

    private static RequestPostProcessor desdeIp(String ip) {
        return peticion -> {
            peticion.setRemoteAddr(ip);
            return peticion;
        };
    }
}
