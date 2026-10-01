package sv.clinica.landing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba los endpoints con los datos iniciales, sobre H2 en memoria. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("Europe/Madrid"));

    private String cita(String fecha, String hora, long tratamientoId) {
        return """
                {"nombre":"Lucía Gómez","telefono":"600 123 456","email":"lucia@example.com",
                 "tratamiento_id":%d,"fecha_preferida":"%s","hora_preferida":%s}
                """.formatted(tratamientoId, fecha, hora == null ? "null" : "\"" + hora + "\"");
    }

    @Test
    void health() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Backend funcionando correctamente."));
    }

    @Test
    void tratamientosYOdontologos() throws Exception {
        mvc.perform(get("/api/tratamientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[0].precio_desde").exists())
                .andExpect(jsonPath("$[0].descripcion_corta").exists());
        mvc.perform(get("/api/tratamientos/1")).andExpect(status().isOk());
        mvc.perform(get("/api/tratamientos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/odontologos")).andExpect(status().isOk());
        mvc.perform(get("/api/odontologos/999")).andExpect(status().isNotFound());
    }

    @Test
    void contacto() throws Exception {
        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Lucía Gómez","telefono":"+34 600 123 456","email":"lucia@example.com","tratamiento":"Limpieza dental"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Solicitud enviada correctamente."));
        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"","telefono":"12ab","email":"no-es-email"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio."))
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    void citas() throws Exception {
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(cita(HOY.plusDays(2).toString(), "10:30", 2)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(cita(HOY.minusDays(1).toString(), null, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha_preferida").exists());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(cita(HOY.plusDays(2).toString(), "21:00", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hora_preferida").exists());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(cita(HOY.plusDays(2).toString(), null, 999)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tratamiento_id").exists());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cors() throws Exception {
        mvc.perform(options("/api/citas").header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"));
        mvc.perform(options("/api/citas").header("Origin", "http://otro-sitio.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
