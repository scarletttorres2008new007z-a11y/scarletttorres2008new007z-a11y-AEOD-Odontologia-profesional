package sv.clinica.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints básicos con los datos iniciales. */
class ApiIntegrationTest extends PruebaIntegracion {

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
                .andExpect(jsonPath("$[0].duracion_minutos").value(30))
                .andExpect(jsonPath("$[0].odontologo_ids", hasSize(0)))
                .andExpect(jsonPath("$[1].odontologo_ids", hasSize(2)));
        mvc.perform(get("/api/tratamientos/1")).andExpect(status().isOk());
        mvc.perform(get("/api/tratamientos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/tratamientos/999"))
                .andExpect(jsonPath("$.timestamp").exists());
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
    void disponibilidadValidaParametros() throws Exception {
        mvc.perform(get("/api/disponibilidad").param("fecha", MARTES.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tratamiento_id").exists());
        mvc.perform(get("/api/disponibilidad").param("tratamiento_id", "2").param("fecha", LUNES.minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value("La fecha no puede ser anterior a hoy."));
        mvc.perform(get("/api/disponibilidad").param("tratamiento_id", "2").param("fecha", LUNES.plusDays(400).toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/disponibilidad").param("tratamiento_id", "999").param("fecha", MARTES.toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/disponibilidad").param("tratamiento_id", "2").param("fecha", MARTES.toString())
                        .param("franja", "NOCHE"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/disponibilidad/proximos").param("tratamiento_id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios", hasSize(greaterThan(0))));
    }

    @Test
    void reservaValidaDatos() throws Exception {
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tratamiento_id").exists())
                .andExpect(jsonPath("$.errores.hora_inicio").exists())
                .andExpect(jsonPath("$.errores.nombre").exists());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(tratamiento("Limpieza dental"), odontologo("Dra. Ana Villar"), MARTES, "25:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hora_inicio").exists());
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
