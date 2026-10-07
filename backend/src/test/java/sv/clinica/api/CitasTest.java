package sv.clinica.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.Tratamiento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agenda y citas desde el software, y los cuatro casos de la regla de oro: una sola tabla de citas para la landing y
 * el software. Reloj fijo: lunes 12/10/2026 a las 08:00 (hora de Madrid).
 */
class CitasTest extends PruebaIntegracion {

    /* ─────────────── Regla de oro ─────────────── */

    @Test
    void laCitaDeLaWebApareceEnElSoftwareYRecepcionLaVinculaYLaConfirma() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        long id = idDe(reservarEnLaWeb(limpieza, ana, MARTES, "10:00"));

        // Es el mismo registro: el software la ve tal y como llegó, pendiente y sin ficha
        mvc.perform(conToken(get("/api/citas").param("desde", MARTES.toString()).param("hasta", MARTES.toString()), recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(id))
                .andExpect(jsonPath("$.contenido[0].origen").value("LANDING"))
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Lucía Gómez"))
                .andExpect(jsonPath("$.contenido[0].paciente_id").doesNotExist());
        mvc.perform(conToken(get("/api/citas").param("sin_ficha", "true"), recepcion))
                .andExpect(jsonPath("$.total_elementos").value(1));

        // Y la agenda del día la pinta con el horario de la clínica, los turnos y el almuerzo
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias[0].apertura").value("09:00"))
                .andExpect(jsonPath("$.dias[0].cierre").value("21:00"))
                .andExpect(jsonPath("$.odontologos.length()").value(4))
                .andExpect(jsonPath("$.turnos[?(@.odontologo_id == " + ana.getId() + ")].hora_fin").value(contains("13:00")))
                .andExpect(jsonPath("$.bloqueos[0].tipo").value("ALMUERZO"))
                .andExpect(jsonPath("$.bloqueos[0].hora_inicio").value("12:00"))
                .andExpect(jsonPath("$.citas[0].id").value(id))
                .andExpect(jsonPath("$.solo_su_agenda").value(false));

        // Recepción la vincula a la ficha del paciente: los datos que dejó en la web se conservan
        long paciente = crearPaciente(recepcion, "Lucía", "Gómez Ruiz");
        mvc.perform(conJson(conToken(put("/api/citas/" + id + "/paciente"), recepcion), Map.of("paciente_id", paciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.id").value(paciente))
                .andExpect(jsonPath("$.paciente.nombre").value("Lucía Gómez Ruiz"))
                .andExpect(jsonPath("$.contacto.nombre").value("Lucía Gómez"))
                .andExpect(jsonPath("$.contacto.email").value("lucia@example.com"));
        mvc.perform(conToken(get("/api/citas").param("sin_ficha", "true"), recepcion))
                .andExpect(jsonPath("$.total_elementos").value(0));
        mvc.perform(conToken(get("/api/citas").param("paciente_id", String.valueOf(paciente)), recepcion))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Lucía Gómez Ruiz"));

        // La confirma. En consulta o no asistió solo se marcan a partir del día de la cita
        mvc.perform(conJson(conToken(put("/api/citas/" + id + "/estado"), recepcion), Map.of("estado", "CONFIRMADA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.estados_siguientes", empty()))
                .andExpect(jsonPath("$.modificable").value(true));
        mvc.perform(conJson(conToken(put("/api/citas/" + id + "/estado"), recepcion), Map.of("estado", "EN_ATENCION")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Eso solo se puede marcar a partir del día de la cita."));

        assertThat(auditoria(id)).containsExactly(
                "RESERVAR LANDING -", "VINCULAR_PACIENTE SOFTWARE recep", "CAMBIAR_ESTADO SOFTWARE recep");
        assertThat(jdbc.queryForObject("select valor_nuevo from auditoria where accion = 'CAMBIAR_ESTADO'", String.class))
                .isEqualTo("{\"estado\":\"CONFIRMADA\"}");
    }

    @Test
    void laCitaQueDaLaClinicaOcupaElHuecoEnLaLanding() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        long paciente = crearPaciente(recepcion, "José Luis", "García Pérez");

        // Huecos para el personal: cada odontóloga por separado
        mvc.perform(conToken(get("/api/agenda/disponibilidad").param("tratamiento_id", limpieza.getId().toString())
                        .param("fecha", MARTES.toString()), recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.hora_inicio == '10:00')].odontologo")
                        .value(contains("Dra. Rocío Fernández", "Dra. Ana Villar")));
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).contains("10:00");

        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), recepcion),
                        nuevaCita(limpieza, ana, MARTES, "10:00", "Prefiere que la llamen por la tarde")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.origen").value("SOFTWARE"))
                .andExpect(jsonPath("$.hora_fin").value("11:00"))
                .andExpect(jsonPath("$.duracion_minutos").value(60))
                .andExpect(jsonPath("$.paciente.id").value(paciente))
                .andExpect(jsonPath("$.contacto.telefono").value("600123456"))
                .andExpect(jsonPath("$.notas_internas").value("Prefiere que la llamen por la tarde"))
                .andExpect(jsonPath("$.creada_por").value("Prueba recep"));

        // La landing ya no ofrece ese hueco y, si alguien lo intenta, la base de datos lo impide
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).doesNotContain("10:00");
        assertThat(horasEnLaWeb(limpieza, rocio, MARTES)).contains("10:00");
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(citaJson(limpieza, ana, MARTES, "10:00")))
                .andExpect(status().isConflict());
        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), recepcion),
                        nuevaCita(limpieza, ana, MARTES, "10:30", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este horario acaba de ser reservado. Selecciona otra opción."));
        assertThat(jdbc.queryForObject("select count(*) from citas", Integer.class)).isEqualTo(1);
    }

    @Test
    void cancelarEnElSoftwareLiberaElHuecoEnLaLanding() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        long paciente = crearPaciente(recepcion, "José Luis", "García Pérez");
        long id = darCita(recepcion, paciente, limpieza, ana, MARTES, "10:00").get("id").asLong();
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).doesNotContain("10:00");

        mvc.perform(conJson(conToken(post("/api/citas/" + id + "/cancelacion"), recepcion), Map.of("motivo", "Avisó por teléfono")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelada_por").value("SOFTWARE"))
                .andExpect(jsonPath("$.motivo_cancelacion").value("Avisó por teléfono"))
                .andExpect(jsonPath("$.modificable").value(false));

        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).contains("10:00");
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), recepcion))
                .andExpect(jsonPath("$.citas", empty()));
        mvc.perform(conJson(conToken(post("/api/citas/" + id + "/cancelacion"), recepcion), Map.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta cita ya no está activa y no se puede cancelar."));
        assertThat(jdbc.queryForObject("select valor_anterior from auditoria where accion = 'CANCELAR'", String.class))
                .isEqualTo("{\"estado\":\"CONFIRMADA\"}");
        assertThat(jdbc.queryForObject("select valor_nuevo from auditoria where accion = 'CANCELAR'", String.class))
                .isEqualTo("{\"estado\":\"CANCELADA\",\"motivo\":\"Avisó por teléfono\"}");
    }

    @Test
    void cancelarDesdeLaLandingSeVeEnElSoftware() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        String codigo = reservarEnLaWeb(limpieza, ana, MARTES, "10:00");
        long id = idDe(codigo);

        // Sin sesión: el paciente se identifica con el código que recibió al reservar
        mvc.perform(post("/api/citas/cancelacion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("codigo", codigo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tu cita se ha cancelado. Ese horario vuelve a estar libre."))
                .andExpect(jsonPath("$.datos.estado").value("CANCELADA"));

        mvc.perform(conToken(get("/api/citas/" + id), recepcion))
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelada_por").value("LANDING"));
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).contains("10:00");

        mvc.perform(post("/api/citas/cancelacion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("codigo", "no-existe"))))
                .andExpect(status().isNotFound());

        // Con menos de 4 horas de antelación, el paciente tiene que llamar
        String hoy = reservarEnLaWeb(limpieza, ana, LUNES, "11:00");
        mvc.perform(post("/api/citas/cancelacion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("codigo", hoy))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Solo se puede cancelar con al menos 4 horas de antelación. Llama a la clínica."));
    }

    /* ─────────────── Reprogramar, estados y notas ─────────────── */

    @Test
    void reprogramarMueveLaCitaYConservaLaFichaYLasNotas() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        long paciente = crearPaciente(recepcion, "José Luis", "García Pérez");
        JsonNode original = darCita(recepcion, paciente, limpieza, ana, MARTES, "10:00", "Trae su radiografía");
        long id = original.get("id").asLong();

        // Al moverla, su propio hueco cuenta como libre: se puede retrasar media hora
        assertThat(huecos(recepcion, limpieza, ana, MARTES, null)).containsExactly("09:00", "11:00");
        assertThat(huecos(recepcion, limpieza, ana, MARTES, id)).containsExactly("09:00", "09:30", "10:00", "10:30", "11:00");
        mvc.perform(conJson(conToken(post("/api/citas/" + id + "/reprogramacion"), recepcion),
                        Map.of("odontologo_id", ana.getId(), "fecha", MARTES.toString(), "hora_inicio", "10:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hora_inicio").value("La cita ya está a esa hora. Elige otro horario."));

        String nueva = mvc.perform(conJson(conToken(post("/api/citas/" + id + "/reprogramacion"), recepcion),
                        Map.of("odontologo_id", ana.getId(), "fecha", MARTES.toString(), "hora_inicio", "10:30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hora_inicio").value("10:30"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.paciente.id").value(paciente))
                .andExpect(jsonPath("$.notas_internas").value("Trae su radiografía"))
                .andExpect(jsonPath("$.cita_anterior_id").value(id))
                .andReturn().getResponse().getContentAsString();
        long nuevaId = leer(nueva).get("id").asLong();

        mvc.perform(conToken(get("/api/citas/" + id), recepcion))
                .andExpect(jsonPath("$.estado").value("REPROGRAMADA"))
                .andExpect(jsonPath("$.cita_nueva_id").value(nuevaId))
                .andExpect(jsonPath("$.modificable").value(false));
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), recepcion))
                .andExpect(jsonPath("$.citas.length()").value(1))
                .andExpect(jsonPath("$.citas[0].id").value(nuevaId));

        // Un odontólogo que no hace el tratamiento, o un hueco ocupado: no cambia nada
        mvc.perform(conJson(conToken(post("/api/citas/" + nuevaId + "/reprogramacion"), recepcion),
                        Map.of("odontologo_id", odontologo("Dr. Marcos Ortega").getId(), "fecha", MARTES.toString(),
                                "hora_inicio", "15:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.odontologo_id").value("Este odontólogo no realiza el tratamiento seleccionado."));
        reservarEnLaWeb(limpieza, ana, MIERCOLES, "09:00");
        mvc.perform(conJson(conToken(post("/api/citas/" + nuevaId + "/reprogramacion"), recepcion),
                        Map.of("odontologo_id", ana.getId(), "fecha", MIERCOLES.toString(), "hora_inicio", "09:00")))
                .andExpect(status().isConflict());
        mvc.perform(conToken(get("/api/citas/" + nuevaId), recepcion))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.fecha").value(MARTES.toString()));
    }

    @Test
    void losEstadosSiguenElOrdenDeLaConsulta() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        long paciente = crearPaciente(recepcion, "José Luis", "García Pérez");
        long hoy = darCita(recepcion, paciente, limpieza, ana, LUNES, "09:00").get("id").asLong();

        mvc.perform(conToken(get("/api/citas/" + hoy), recepcion))
                .andExpect(jsonPath("$.estados_siguientes", contains("EN_ATENCION", "NO_ASISTIO")));
        estado(recepcion, hoy, "EN_ATENCION").andExpect(status().isOk())
                .andExpect(jsonPath("$.estados_siguientes", contains("COMPLETADA")))
                .andExpect(jsonPath("$.modificable").value(false));
        estado(recepcion, hoy, "COMPLETADA").andExpect(status().isOk())
                .andExpect(jsonPath("$.estados_siguientes", empty()));
        estado(recepcion, hoy, "CONFIRMADA").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta cita está completada y no se puede pasar a confirmada."));
        mvc.perform(conJson(conToken(post("/api/citas/" + hoy + "/cancelacion"), recepcion), Map.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta cita ya está completada y no se puede cancelar."));
        estado(recepcion, hoy, "CANCELADA").andExpect(status().isBadRequest());

        // No asistió se puede deshacer si se marcó por error, y la cita conserva su hueco
        long web = idDe(reservarEnLaWeb(limpieza, ana, LUNES, "11:00"));
        estado(recepcion, web, "NO_ASISTIO").andExpect(status().isOk());
        estado(recepcion, web, "CONFIRMADA").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from agenda_ocupacion where cita_id = ?", Integer.class, web))
                .isEqualTo(4);

        // Notas internas: se guardan, se pueden borrar y cada cambio queda en la auditoría
        mvc.perform(conJson(conToken(put("/api/citas/" + web + "/notas"), recepcion), Map.of("notas_internas", " Llega tarde ")))
                .andExpect(jsonPath("$.notas_internas").value("Llega tarde"));
        mvc.perform(conJson(conToken(put("/api/citas/" + web + "/notas"), recepcion), Map.of("notas_internas", "")))
                .andExpect(jsonPath("$.notas_internas").doesNotExist());
        assertThat(jdbc.queryForObject("select count(*) from auditoria where accion = 'EDITAR' and entidad = 'CITA'",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void elBackendRevisaLosDatosDeLaCita() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        long paciente = crearPaciente(recepcion, "José Luis", "García Pérez");

        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), recepcion), Map.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tratamiento_id").value("Elige el tratamiento."))
                .andExpect(jsonPath("$.errores.odontologo_id").value("Elige un horario."))
                .andExpect(jsonPath("$.errores.fecha").value("Elige el día."))
                .andExpect(jsonPath("$.errores.hora_inicio").value("Elige un horario."));
        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), recepcion),
                        nuevaCita(limpieza, odontologo("Dr. Marcos Ortega"), MARTES, "15:00", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.odontologo_id").value("Este odontólogo no realiza el tratamiento seleccionado."));
        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), recepcion),
                        nuevaCita(limpieza, odontologo("Dra. Ana Villar"), LUNES.minusDays(1), "10:00", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value("La fecha no puede ser anterior a hoy."));
        mvc.perform(conJson(conToken(post("/api/pacientes/999999/citas"), recepcion),
                        nuevaCita(limpieza, odontologo("Dra. Ana Villar"), MARTES, "10:00", null)))
                .andExpect(status().isNotFound());

        // A un paciente de baja no se le da cita
        mvc.perform(conJson(conToken(put("/api/pacientes/" + paciente + "/estado"), recepcion), Map.of("activo", false)))
                .andExpect(status().isOk());
        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), recepcion),
                        nuevaCita(limpieza, odontologo("Dra. Ana Villar"), MARTES, "10:00", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este paciente está de baja. Reactívalo desde su ficha para darle cita."));
    }

    /* ─────────────── Permisos ─────────────── */

    @Test
    void elOdontologoSoloVeSuAgendaYElBackendLoComprueba() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        long paciente = crearPaciente(recepcion, "José Luis", "García Pérez");
        long deAna = darCita(recepcion, paciente, limpieza, ana, MARTES, "10:00").get("id").asLong();
        long deRocio = darCita(recepcion, paciente, limpieza, rocio, MARTES, "10:00").get("id").asLong();

        // La Dra. Ana tiene su usuario vinculado a su agenda; el rol de odontólogo solo trae «ver citas»
        long usuarioAna = crearUsuario("dra.ana", "ODONTOLOGO").getId();
        jdbc.update("update odontologos set usuario_id = ? where id = ?", usuarioAna, ana.getId());
        String draAna = entrar("dra.ana");

        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), draAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solo_su_agenda").value(true))
                .andExpect(jsonPath("$.odontologos.length()").value(1))
                .andExpect(jsonPath("$.odontologos[0].id").value(ana.getId()))
                .andExpect(jsonPath("$.citas.length()").value(1))
                .andExpect(jsonPath("$.citas[0].id").value(deAna));
        // Aunque pida la agenda de otra odontóloga, recibe la suya
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString())
                        .param("odontologo_id", rocio.getId().toString()), draAna))
                .andExpect(jsonPath("$.citas[0].id").value(deAna));
        mvc.perform(conToken(get("/api/citas"), draAna))
                .andExpect(jsonPath("$.total_elementos").value(1));
        mvc.perform(conToken(get("/api/citas/" + deAna), draAna)).andExpect(status().isOk());
        // La cita de otra agenda es como si no existiera, aunque escriba su número a mano
        mvc.perform(conToken(get("/api/citas/" + deRocio), draAna)).andExpect(status().isNotFound());

        // Lo que su rol no permite, la API lo rechaza aunque se llame directamente
        mvc.perform(conJson(conToken(put("/api/citas/" + deAna + "/estado"), draAna), Map.of("estado", "CONFIRMADA")))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(post("/api/citas/" + deAna + "/cancelacion"), draAna), Map.of()))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), draAna),
                        nuevaCita(limpieza, ana, MARTES, "11:00", null)))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/agenda/disponibilidad").param("tratamiento_id", limpieza.getId().toString())
                        .param("fecha", MARTES.toString()), draAna))
                .andExpect(status().isForbidden());

        // Con permiso para dar citas, solo en su agenda
        darPermisos("ODONTOLOGO", "citas.crear");
        mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), draAna),
                        nuevaCita(limpieza, rocio, MARTES, "15:00", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.odontologo_id").value("Solo puedes dar o mover citas en tu agenda."));

        // Un odontólogo sin agenda vinculada no ve ninguna
        crearUsuario("dr.nuevo", "ODONTOLOGO");
        String nuevo = entrar("dr.nuevo");
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), nuevo))
                .andExpect(jsonPath("$.odontologos", empty()))
                .andExpect(jsonPath("$.citas", empty()));
        mvc.perform(conToken(get("/api/citas/" + deAna), nuevo)).andExpect(status().isNotFound());

        // Sin rol, 403; sin sesión, 401
        crearUsuario("sin.rol");
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), entrar("sin.rol")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/agenda").param("desde", MARTES.toString())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/citas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/citas/" + deAna)).andExpect(status().isUnauthorized());
    }

    /* ─────────────── Ayudas ─────────────── */

    private String recepcion() throws Exception {
        crearUsuario("recep", "RECEPCION");
        return entrar("recep");
    }

    private long crearPaciente(String token, String nombres, String apellidos) throws Exception {
        String cuerpo = mvc.perform(conJson(conToken(post("/api/pacientes"), token),
                        Map.of("nombres", nombres, "apellidos", apellidos, "telefono", "600 123 456")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return leer(cuerpo).get("id").asLong();
    }

    private Map<String, Object> nuevaCita(Tratamiento t, Odontologo o, LocalDate fecha, String hora, String notas) {
        Map<String, Object> datos = new HashMap<>(Map.of("tratamiento_id", t.getId(), "odontologo_id", o.getId(),
                "fecha", fecha.toString(), "hora_inicio", hora));
        if (notas != null) datos.put("notas_internas", notas);
        return datos;
    }

    private JsonNode darCita(String token, long paciente, Tratamiento t, Odontologo o, LocalDate fecha, String hora)
            throws Exception {
        return darCita(token, paciente, t, o, fecha, hora, null);
    }

    private JsonNode darCita(String token, long paciente, Tratamiento t, Odontologo o, LocalDate fecha, String hora,
                             String notas) throws Exception {
        String cuerpo = mvc.perform(conJson(conToken(post("/api/pacientes/" + paciente + "/citas"), token),
                        nuevaCita(t, o, fecha, hora, notas)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return leer(cuerpo);
    }

    /** Reserva como un paciente en la web y devuelve el código que recibe. */
    private String reservarEnLaWeb(Tratamiento t, Odontologo o, LocalDate fecha, String hora) throws Exception {
        String cuerpo = mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content(citaJson(t, o, fecha, hora)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return leer(cuerpo).get("datos").get("codigo").asText();
    }

    private long idDe(String codigo) {
        return jdbc.queryForObject("select id from citas where codigo = ?", Long.class, codigo);
    }

    /** Las horas que la landing ofrece ese día (sin sesión, como cualquier visitante). */
    private List<String> horasEnLaWeb(Tratamiento t, Odontologo o, LocalDate fecha) throws Exception {
        String cuerpo = mvc.perform(get("/api/disponibilidad").param("tratamiento_id", t.getId().toString())
                        .param("fecha", fecha.toString()).param("odontologo_id", o.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> horas = new ArrayList<>();
        leer(cuerpo).get("horarios").forEach(h -> horas.add(h.get("hora_inicio").asText()));
        return horas;
    }

    private List<String> huecos(String token, Tratamiento t, Odontologo o, LocalDate fecha, Long excluir) throws Exception {
        var peticion = get("/api/agenda/disponibilidad").param("tratamiento_id", t.getId().toString())
                .param("fecha", fecha.toString()).param("odontologo_id", o.getId().toString());
        if (excluir != null) peticion.param("excluir_cita_id", excluir.toString());
        String cuerpo = mvc.perform(conToken(peticion, token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> horas = new ArrayList<>();
        leer(cuerpo).forEach(h -> horas.add(h.get("hora_inicio").asText()));
        return horas;
    }

    private org.springframework.test.web.servlet.ResultActions estado(String token, long cita, String estado) throws Exception {
        return mvc.perform(conJson(conToken(put("/api/citas/" + cita + "/estado"), token), Map.of("estado", estado)));
    }

    /** "ACCION ORIGEN usuario" de cada registro de auditoría de la cita, en orden. */
    private List<String> auditoria(long cita) {
        return jdbc.queryForList("""
                select concat(a.accion, ' ', a.origen, ' ', coalesce(u.username, '-')) from auditoria a
                left join usuarios u on u.id = a.usuario_id
                where a.entidad = 'CITA' and a.entidad_id = ? order by a.id""", String.class, String.valueOf(cita));
    }
}
