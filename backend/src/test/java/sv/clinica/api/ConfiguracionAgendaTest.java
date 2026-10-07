package sv.clinica.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.Tratamiento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configurar la agenda desde el software: odontólogos, tratamientos, horarios y bloqueos. Cada cambio se nota al
 * momento en los huecos que ofrece la landing, y las citas que ya había nunca se cancelan solas.
 * Reloj fijo: lunes 12/10/2026 a las 08:00 (hora de Madrid).
 */
class ConfiguracionAgendaTest extends PruebaIntegracion {

    private static final String BLOQUEOS = "/api/configuracion/bloqueos";
    private static final String HORARIOS = "/api/configuracion/horarios";
    private static final String TRATAMIENTOS = "/api/configuracion/tratamientos";
    private static final String ODONTOLOGOS = "/api/configuracion/odontologos";

    /* ─────────────── Permisos ─────────────── */

    @Test
    void cadaPantallaDeConfiguracionPideSuPermiso() throws Exception {
        crearUsuario("odonto", "ODONTOLOGO");
        crearUsuario("recep", "RECEPCION");
        crearUsuario("coord", "COORDINADOR");
        String odontologo = entrar("odonto");
        String recepcion = entrar("recep");
        String coordinacion = entrar("coord");

        for (String ruta : List.of(BLOQUEOS, HORARIOS, TRATAMIENTOS, ODONTOLOGOS)) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mvc.perform(conToken(get(ruta), odontologo)).andExpect(status().isForbidden());
            mvc.perform(conToken(get(ruta), coordinacion)).andExpect(status().isOk());
        }
        // Recepción puede bloquear tiempo, pero no cambiar horarios, tratamientos ni odontólogos
        mvc.perform(conToken(get(BLOQUEOS), recepcion)).andExpect(status().isOk());
        mvc.perform(conToken(get(HORARIOS), recepcion)).andExpect(status().isForbidden());
        mvc.perform(conToken(get(TRATAMIENTOS), recepcion)).andExpect(status().isForbidden());
        mvc.perform(conToken(get(ODONTOLOGOS), recepcion)).andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(put(HORARIOS + "/clinica"), recepcion), Map.of("dias", List.of())))
                .andExpect(status().isForbidden());
        // Y nada ha cambiado: la clínica sigue abierta
        assertThat(jdbc.queryForObject("select count(*) from horarios_clinica", Integer.class)).isEqualTo(6);
    }

    /* ─────────────── Bloqueos ─────────────── */

    @Test
    void unFestivoCierraElDiaEnLaWebYAvisaDeLasCitasQueYaHabia() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        long cita = idDe(reservarEnLaWeb(limpieza, ana, MARTES, "10:00"));
        assertThat(horasEnLaWeb(limpieza, rocio, MARTES)).isNotEmpty();

        String cuerpo = mvc.perform(conJson(conToken(post(BLOQUEOS), recepcion),
                        Map.of("tipo", "FERIADO", "motivo", "Fiesta local", "fecha_inicio", MARTES.toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.tipo").value("FERIADO"))
                .andExpect(jsonPath("$.bloqueo.fecha_inicio").value(MARTES.toString()))
                .andExpect(jsonPath("$.bloqueo.fecha_fin").value(MARTES.toString()))
                .andExpect(jsonPath("$.bloqueo.odontologo_id").doesNotExist())
                .andExpect(jsonPath("$.bloqueo.hora_inicio").doesNotExist())
                // La cita no se cancela: se avisa para que recepción la mueva o la cancele
                .andExpect(jsonPath("$.citas_afectadas.total").value(1))
                .andExpect(jsonPath("$.citas_afectadas.citas[0].id").value(cita))
                .andReturn().getResponse().getContentAsString();
        long bloqueo = leer(cuerpo).get("bloqueo").get("id").asLong();

        assertThat(horasEnLaWeb(limpieza, rocio, MARTES)).isEmpty();
        assertThat(horasEnLaWeb(limpieza, rocio, MIERCOLES)).isNotEmpty();
        assertThat(jdbc.queryForObject("select estado from citas where id = ?", String.class, cita)).isEqualTo("PENDIENTE");
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), recepcion))
                .andExpect(jsonPath("$.bloqueos[*].tipo", hasItem("FERIADO")));
        // En la lista, primero los semanales (el almuerzo) y después los de fechas
        mvc.perform(conToken(get(BLOQUEOS), recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].tipo").value("ALMUERZO"))
                .andExpect(jsonPath("$[5].id").value(bloqueo))
                .andExpect(jsonPath("$[5].motivo").value("Fiesta local"));

        // Quitarlo no lo borra: queda desactivado, en la auditoría, y el día vuelve a estar libre al momento
        mvc.perform(conToken(delete(BLOQUEOS + "/" + bloqueo), recepcion)).andExpect(status().isNoContent());
        assertThat(horasEnLaWeb(limpieza, rocio, MARTES)).isNotEmpty();
        assertThat(jdbc.queryForObject("select activo from bloqueos where id = ?", Boolean.class, bloqueo)).isFalse();
        mvc.perform(conToken(get(BLOQUEOS), recepcion)).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(conToken(delete(BLOQUEOS + "/" + bloqueo), recepcion)).andExpect(status().isNotFound());
        assertThat(auditoria("BLOQUEO", bloqueo)).containsExactly("CREAR recep", "DESACTIVAR recep");
    }

    @Test
    void unaCapacitacionDeUnaOdontologaSoloQuitaSusHuecosYSePuedeMover() throws Exception {
        String recepcion = recepcion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).containsExactly("09:00", "09:30", "10:00", "10:30", "11:00");

        Map<String, Object> capacitacion = new HashMap<>(Map.of("tipo", "CAPACITACION", "motivo", "Curso de implantes",
                "odontologo_id", ana.getId(), "fecha_inicio", MARTES.toString(),
                "hora_inicio", "10:00", "hora_fin", "11:00"));
        String cuerpo = mvc.perform(conJson(conToken(post(BLOQUEOS), recepcion), capacitacion))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.tipo").value("CAPACITACION"))
                .andExpect(jsonPath("$.bloqueo.odontologo").value("Dra. Ana Villar"))
                .andExpect(jsonPath("$.bloqueo.hora_inicio").value("10:00"))
                .andExpect(jsonPath("$.citas_afectadas.total").value(0))
                .andReturn().getResponse().getContentAsString();
        long bloqueo = leer(cuerpo).get("bloqueo").get("id").asLong();
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).containsExactly("09:00", "11:00");
        assertThat(horasEnLaWeb(limpieza, rocio, MARTES)).contains("10:00", "10:30");

        capacitacion.put("hora_inicio", "11:00");
        capacitacion.put("hora_fin", "12:00");
        mvc.perform(conJson(conToken(put(BLOQUEOS + "/" + bloqueo), recepcion), capacitacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueo.hora_inicio").value("11:00"));
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).containsExactly("09:00", "09:30", "10:00");
        assertThat(jdbc.queryForObject("select valor_nuevo from auditoria where accion = 'EDITAR' and entidad = 'BLOQUEO'",
                String.class)).isEqualTo("{\"horas\":\"11:00 a 12:00\"}");
    }

    @Test
    void sinVerTodasLasAgendasSoloSeDiceCuantasCitasQuedanDentro() throws Exception {
        // Un odontólogo al que se le deja bloquear tiempo no ve los datos de las citas de los demás
        darPermisos("ODONTOLOGO", "bloqueos.gestionar");
        crearUsuario("odonto", "ODONTOLOGO");
        String odontologo = entrar("odonto");
        reservarEnLaWeb(tratamiento("Limpieza dental"), odontologo("Dra. Ana Villar"), MARTES, "10:00");

        mvc.perform(conJson(conToken(post(BLOQUEOS), odontologo),
                        Map.of("tipo", "MANTENIMIENTO", "fecha_inicio", MARTES.toString(),
                                "hora_inicio", "09:00", "hora_fin", "12:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.citas_afectadas.total").value(1))
                .andExpect(jsonPath("$.citas_afectadas.citas").doesNotExist());
    }

    @Test
    void losBloqueosSeValidanEnElBackend() throws Exception {
        String recepcion = recepcion();
        String martes = MARTES.toString();
        bloqueoInvalido(recepcion, Map.of("motivo", "Sin tipo", "fecha_inicio", martes), "tipo");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL"), "fecha_inicio");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL", "fecha_fin", martes), "fecha_inicio");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL", "fecha_inicio", martes, "fecha_fin", LUNES.toString()),
                "fecha_fin");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL", "fecha_inicio", LUNES.minusDays(3).toString(),
                "fecha_fin", LUNES.minusDays(1).toString()), "fecha_fin");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL", "fecha_inicio", martes, "hora_inicio", "10:00"), "hora_fin");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL", "fecha_inicio", martes,
                "hora_inicio", "10:10", "hora_fin", "11:00"), "hora_inicio");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL", "fecha_inicio", martes,
                "hora_inicio", "11:00", "hora_fin", "11:00"), "hora_fin");
        bloqueoInvalido(recepcion, Map.of("tipo", "MANUAL", "fecha_inicio", martes, "odontologo_id", 9999), "odontologo_id");
        bloqueoInvalido(recepcion, Map.of("tipo", "REUNION", "dia_semana", 8), "dia_semana");
        assertThat(jdbc.queryForObject("select count(*) from bloqueos", Integer.class)).isEqualTo(5);

        // Una reunión semanal sin fechas sí vale: cada miércoles de 9:00 a 10:00
        mvc.perform(conJson(conToken(post(BLOQUEOS), recepcion),
                        Map.of("tipo", "REUNION", "dia_semana", 3, "hora_inicio", "09:00", "hora_fin", "10:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.dia_semana").value(3))
                .andExpect(jsonPath("$.bloqueo.fecha_inicio").doesNotExist());
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        assertThat(horasEnLaWeb(limpieza, rocio, MIERCOLES)).first().isEqualTo("10:00");

        // Cada semana a partir de un día, sin último día: sigue sin fin (no se queda en ese único día)
        LocalDate siguiente = MIERCOLES.plusWeeks(1);
        mvc.perform(conJson(conToken(post(BLOQUEOS), recepcion), Map.of("tipo", "CAPACITACION",
                        "dia_semana", 3, "fecha_inicio", siguiente.toString(), "hora_inicio", "10:00", "hora_fin", "11:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.fecha_inicio").value(siguiente.toString()))
                .andExpect(jsonPath("$.bloqueo.fecha_fin").doesNotExist());
        assertThat(horasEnLaWeb(limpieza, rocio, MIERCOLES)).first().isEqualTo("10:00");
        assertThat(horasEnLaWeb(limpieza, rocio, siguiente)).first().isEqualTo("11:00");
        assertThat(horasEnLaWeb(limpieza, rocio, siguiente.plusWeeks(4))).first().isEqualTo("11:00");
    }

    /* ─────────────── Horarios y turnos ─────────────── */

    @Test
    void cambiarElHorarioDeLaClinicaSeNotaEnLaWebYAvisaDeLasCitasQueQuedanFuera() throws Exception {
        String coordinacion = coordinacion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        long cita = idDe(reservarEnLaWeb(limpieza, ana, MARTES, "10:00"));

        mvc.perform(conToken(get(HORARIOS), coordinacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinica.length()").value(6))
                .andExpect(jsonPath("$.clinica[0].hora_apertura").value("09:00"))
                .andExpect(jsonPath("$.odontologos.length()").value(4));

        // El martes abre a las 11:00: la cita de las 10:00 queda fuera, pero no se cancela
        List<Map<String, Object>> semana = semanaDeEjemplo();
        semana.set(1, dia(2, "11:00", "21:00"));
        mvc.perform(conJson(conToken(put(HORARIOS + "/clinica"), coordinacion), Map.of("dias", semana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios.clinica[1].hora_apertura").value("11:00"))
                .andExpect(jsonPath("$.citas_afectadas.total").value(1))
                .andExpect(jsonPath("$.citas_afectadas.citas[0].id").value(cita));
        assertThat(horasEnLaWeb(limpieza, rocio, MARTES)).first().isEqualTo("11:00");
        assertThat(jdbc.queryForObject("select estado from citas where id = ?", String.class, cita)).isEqualTo("PENDIENTE");
        assertThat(jdbc.queryForObject(
                "select concat(valor_anterior, ' → ', valor_nuevo) from auditoria where entidad = 'HORARIO_CLINICA'",
                String.class)).isEqualTo("{\"martes\":\"09:00 a 21:00\"} → {\"martes\":\"11:00 a 21:00\"}");

        // Sin el martes en la lista, el martes queda cerrado
        semana.remove(1);
        mvc.perform(conJson(conToken(put(HORARIOS + "/clinica"), coordinacion), Map.of("dias", semana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios.clinica.length()").value(5));
        assertThat(horasEnLaWeb(limpieza, rocio, MARTES)).isEmpty();
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), coordinacion))
                .andExpect(jsonPath("$.dias[0].apertura").doesNotExist());

        // Lo que no tiene sentido se rechaza y no cambia nada
        horarioInvalido(coordinacion, List.of(dia(1, "21:00", "09:00")), "La hora de fin tiene que ser posterior");
        horarioInvalido(coordinacion, List.of(dia(1, "09:10", "21:00")), "Usa horas en punto o en cuartos");
        horarioInvalido(coordinacion, List.of(dia(1, "09:00", "14:00"), dia(1, "16:00", "21:00")), "El lunes aparece dos veces.");
        horarioInvalido(coordinacion, List.of(dia(9, "09:00", "14:00")), "El día de la semana no es válido.");
        assertThat(jdbc.queryForObject("select count(*) from horarios_clinica", Integer.class)).isEqualTo(5);
    }

    @Test
    void losTurnosDeUnaOdontologaCambianSusHuecos() throws Exception {
        String coordinacion = coordinacion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        long cita = idDe(reservarEnLaWeb(limpieza, ana, MARTES, "10:00"));

        // Ahora trabaja los martes por la tarde, en dos tramos
        List<Map<String, Object>> turnos = List.of(turno(2, "15:00", "17:00"), turno(2, "17:00", "19:00"));
        mvc.perform(conJson(conToken(put(HORARIOS + "/odontologos/" + ana.getId()), coordinacion), Map.of("turnos", turnos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios.odontologos[?(@.id == " + ana.getId() + ")].turnos.length()").value(2))
                .andExpect(jsonPath("$.citas_afectadas.total").value(1))
                .andExpect(jsonPath("$.citas_afectadas.citas[0].id").value(cita));
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).first().isEqualTo("15:00");
        assertThat(horasEnLaWeb(limpieza, ana, MIERCOLES)).isEmpty();
        assertThat(jdbc.queryForObject(
                "select valor_nuevo from auditoria where entidad = 'HORARIO_ODONTOLOGO' and entidad_id = ?",
                String.class, ana.getId().toString()))
                .isEqualTo("{\"lunes\":\"No trabaja\",\"martes\":\"15:00 a 17:00 y 17:00 a 19:00\",\"miércoles\":\"No trabaja\","
                        + "\"jueves\":\"No trabaja\",\"viernes\":\"No trabaja\",\"sábado\":\"No trabaja\"}");

        mvc.perform(conJson(conToken(put(HORARIOS + "/odontologos/" + ana.getId()), coordinacion),
                        Map.of("turnos", List.of(turno(4, "09:00", "13:00"), turno(4, "12:00", "15:00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.turnos").value("Los turnos del jueves se pisan: 09:00 a 13:00 y 12:00 a 15:00."));
        mvc.perform(conJson(conToken(put(HORARIOS + "/odontologos/9999"), coordinacion), Map.of("turnos", List.of())))
                .andExpect(status().isNotFound());
    }

    /* ─────────────── Tratamientos ─────────────── */

    @Test
    void cambiarLaDuracionYQuienHaceUnTratamientoCambiaLosHuecosNuevos() throws Exception {
        String coordinacion = coordinacion();
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        long cita = idDe(reservarEnLaWeb(limpieza, ana, MARTES, "10:00"));

        Map<String, Object> datos = new HashMap<>(Map.of("nombre", "Limpieza dental",
                "descripcion_corta", "Higiene profesional y revisión.", "precio_desde", 60, "duracion_aproximada", "45 min",
                "duracion_minutos", 45, "odontologo_ids", List.of(ana.getId(), rocio.getId())));
        mvc.perform(conJson(conToken(put(TRATAMIENTOS + "/" + limpieza.getId()), coordinacion), datos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracion_minutos").value(45));
        // La landing la recibe al momento; la cita que ya estaba dada no cambia
        mvc.perform(get("/api/tratamientos"))
                .andExpect(jsonPath("$[?(@.nombre == 'Limpieza dental')].duracion_minutos").value(contains(45)));
        assertThat(horasEnLaWeb(limpieza, ana, MARTES)).containsExactly("09:00", "11:00");
        assertThat(jdbc.queryForObject("select hora_fin from citas where id = ?", String.class, cita)).startsWith("11:00");
        assertThat(jdbc.queryForObject(
                "select concat(valor_anterior, ' → ', valor_nuevo) from auditoria where entidad = 'TRATAMIENTO'",
                String.class)).isEqualTo("{\"duracion_aproximada\":\"60 min\",\"duracion_minutos\":60} → "
                        + "{\"duracion_aproximada\":\"45 min\",\"duracion_minutos\":45}");

        // Solo la Dra. Rocío: la web deja de ofrecer a la Dra. Ana para la limpieza
        datos.put("odontologo_ids", List.of(rocio.getId()));
        mvc.perform(conJson(conToken(put(TRATAMIENTOS + "/" + limpieza.getId()), coordinacion), datos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.odontologos[*].id", contains(rocio.getId().intValue())))
                .andExpect(jsonPath("$.odontologos[0].nombre").value("Dra. Rocío Fernández"))
                .andExpect(jsonPath("$.odontologos[0].activo").value(true));
        mvc.perform(get("/api/disponibilidad").param("tratamiento_id", limpieza.getId().toString())
                        .param("fecha", MARTES.toString()))
                .andExpect(jsonPath("$.horarios[*].odontologo_id", not(hasItem(ana.getId().intValue()))))
                .andExpect(jsonPath("$.horarios[*].odontologo_id", hasItem(rocio.getId().intValue())));

        datos.put("duracion_minutos", 50);
        tratamientoInvalido(coordinacion, limpieza.getId(), datos, 400, "duracion_minutos");
        datos.put("duracion_minutos", 45);
        datos.put("precio_desde", -5);
        tratamientoInvalido(coordinacion, limpieza.getId(), datos, 400, "precio_desde");
        datos.put("precio_desde", 49);
        datos.put("nombre", "endodoncia");
        tratamientoInvalido(coordinacion, limpieza.getId(), datos, 409, "nombre");
        datos.put("nombre", "Limpieza dental");
        datos.put("odontologo_ids", List.of(9999));
        tratamientoInvalido(coordinacion, limpieza.getId(), datos, 400, "odontologo_ids");
    }

    @Test
    void unTratamientoNuevoSaleEnLaWebYDejarDeOfrecerloNoPierdeCitas() throws Exception {
        String coordinacion = coordinacion();
        String cuerpo = mvc.perform(conJson(conToken(post(TRATAMIENTOS), coordinacion),
                        Map.of("nombre", "Revisión infantil", "duracion_minutos", 30, "precio_desde", 0,
                                "duracion_aproximada", "30 min")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.odontologos.length()").value(0))
                .andReturn().getResponse().getContentAsString();
        long nuevo = leer(cuerpo).get("id").asLong();
        mvc.perform(get("/api/tratamientos")).andExpect(jsonPath("$[*].nombre", hasItem("Revisión infantil")));
        // Sin nadie asignado lo hace cualquiera: hay huecos con cualquier odontólogo
        mvc.perform(get("/api/disponibilidad").param("tratamiento_id", String.valueOf(nuevo)).param("fecha", MARTES.toString()))
                .andExpect(jsonPath("$.horarios[0].hora_inicio").value("09:00"));

        // Mientras tenga citas pendientes no se puede dejar de ofrecer
        Tratamiento limpieza = tratamiento("Limpieza dental");
        reservarEnLaWeb(limpieza, odontologo("Dra. Ana Villar"), MARTES, "10:00");
        mvc.perform(conJson(conToken(put(TRATAMIENTOS + "/" + limpieza.getId() + "/estado"), coordinacion),
                        Map.of("activo", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", startsWith("Tiene 1 cita pendiente o confirmada de hoy en adelante.")));

        mvc.perform(conJson(conToken(put(TRATAMIENTOS + "/" + nuevo + "/estado"), coordinacion), Map.of("activo", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/tratamientos")).andExpect(jsonPath("$[*].nombre", not(hasItem("Revisión infantil"))));
        mvc.perform(conToken(get(TRATAMIENTOS), coordinacion))
                .andExpect(jsonPath("$[?(@.id == " + nuevo + ")].activo").value(contains(false)));
        assertThat(auditoria("TRATAMIENTO", nuevo)).containsExactly("CREAR coord", "DESACTIVAR coord");
    }

    /* ─────────────── Odontólogos ─────────────── */

    @Test
    void unaOdontologaNuevaConSuUsuarioVeSoloSuAgenda() throws Exception {
        String coordinacion = coordinacion();
        String cuerpo = mvc.perform(conJson(conToken(post(ODONTOLOGOS), coordinacion),
                        Map.of("nombre", "Dra. Marta Pérez", "especialidad", "Odontopediatría",
                                "descripcion", "Atiende a niños y adolescentes.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.tratamientos", hasItem("Valoración odontológica")))
                .andExpect(jsonPath("$.tratamientos", not(hasItem("Limpieza dental"))))
                .andReturn().getResponse().getContentAsString();
        long marta = leer(cuerpo).get("id").asLong();
        mvc.perform(get("/api/odontologos")).andExpect(jsonPath("$[*].nombre", hasItem("Dra. Marta Pérez")));

        // Su usuario del software, para que vea su agenda
        long usuario = crearUsuario("marta", "ODONTOLOGO").getId();
        mvc.perform(conToken(get(ODONTOLOGOS + "/usuarios"), coordinacion))
                .andExpect(jsonPath("$[?(@.username == 'marta')].roles[0]").value(contains("Odontólogo")))
                .andExpect(jsonPath("$[?(@.username == 'marta')].odontologo_id").isEmpty());
        Map<String, Object> datos = new HashMap<>(Map.of("nombre", "Dra. Marta Pérez", "especialidad", "Odontopediatría",
                "usuario_id", usuario));
        mvc.perform(conJson(conToken(put(ODONTOLOGOS + "/" + marta), coordinacion), datos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.username").value("marta"))
                .andExpect(jsonPath("$.descripcion").doesNotExist());
        mvc.perform(conToken(get("/api/agenda").param("desde", MARTES.toString()), entrar("marta")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solo_su_agenda").value(true))
                .andExpect(jsonPath("$.odontologos.length()").value(1))
                .andExpect(jsonPath("$.odontologos[0].id").value(marta));

        // Un usuario solo puede ser de un odontólogo
        Odontologo ana = odontologo("Dra. Ana Villar");
        mvc.perform(conJson(conToken(put(ODONTOLOGOS + "/" + ana.getId()), coordinacion),
                        Map.of("nombre", ana.getNombre(), "usuario_id", usuario)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.usuario_id").value("Ese usuario ya está vinculado a Dra. Marta Pérez."));
        mvc.perform(conJson(conToken(post(ODONTOLOGOS), coordinacion), Map.of("nombre", "dra. marta pérez")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.nombre").value("Ya hay un odontólogo con ese nombre."));

        // Con turnos tiene huecos en la web; con citas pendientes no se puede desactivar
        mvc.perform(conJson(conToken(put(HORARIOS + "/odontologos/" + marta), coordinacion),
                        Map.of("turnos", List.of(turno(2, "16:00", "20:00")))))
                .andExpect(status().isOk());
        Tratamiento valoracion = tratamiento("Valoración odontológica");
        Odontologo martaPerez = odontologo("Dra. Marta Pérez");
        assertThat(horasEnLaWeb(valoracion, martaPerez, MARTES)).first().isEqualTo("16:00");
        reservarEnLaWeb(valoracion, martaPerez, MARTES, "16:00");
        cambiarEstado(coordinacion, marta, false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tiene 1 cita pendiente o confirmada de hoy en adelante. "
                        + "Muévelas a otro odontólogo o cancélalas antes de desactivarlo."));
        jdbc.update("update citas set estado = 'CANCELADA', cancelada_en = ?, cancelada_por = 'SOFTWARE'", AHORA);
        cambiarEstado(coordinacion, marta, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/odontologos")).andExpect(jsonPath("$[*].nombre", not(hasItem("Dra. Marta Pérez"))));
        mvc.perform(conToken(get(ODONTOLOGOS), coordinacion)).andExpect(jsonPath("$.length()").value(5));
        assertThat(auditoria("ODONTOLOGO", marta)).containsExactly("CREAR coord", "EDITAR coord", "DESACTIVAR coord");
        assertThat(jdbc.queryForObject("select concat(valor_anterior, ' → ', valor_nuevo) from auditoria "
                + "where entidad = 'ODONTOLOGO' and accion = 'EDITAR'", String.class))
                .isEqualTo("{\"descripcion\":\"Atiende a niños y adolescentes.\"} → {\"usuario\":\"Prueba marta (marta)\"}");
    }

    /* ─────────────── Ayudas ─────────────── */

    private String recepcion() throws Exception {
        crearUsuario("recep", "RECEPCION");
        return entrar("recep");
    }

    private String coordinacion() throws Exception {
        crearUsuario("coord", "COORDINADOR");
        return entrar("coord");
    }

    private void bloqueoInvalido(String token, Map<String, Object> datos, String campo) throws Exception {
        mvc.perform(conJson(conToken(post(BLOQUEOS), token), datos))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores." + campo).exists());
    }

    private void horarioInvalido(String token, List<Map<String, Object>> dias, String mensaje) throws Exception {
        mvc.perform(conJson(conToken(put(HORARIOS + "/clinica"), token), Map.of("dias", dias)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dias", startsWith(mensaje)));
    }

    private void tratamientoInvalido(String token, long id, Map<String, Object> datos, int estado, String campo)
            throws Exception {
        mvc.perform(conJson(conToken(put(TRATAMIENTOS + "/" + id), token), datos))
                .andExpect(status().is(estado))
                .andExpect(jsonPath("$.errores." + campo).exists());
    }

    private ResultActions cambiarEstado(String token, long odontologo, boolean activo) throws Exception {
        return mvc.perform(conJson(conToken(put(ODONTOLOGOS + "/" + odontologo + "/estado"), token),
                Map.of("activo", activo)));
    }

    private static Map<String, Object> dia(int dia, String apertura, String cierre) {
        return Map.of("dia_semana", dia, "hora_apertura", apertura, "hora_cierre", cierre);
    }

    private static Map<String, Object> turno(int dia, String inicio, String fin) {
        return Map.of("dia_semana", dia, "hora_inicio", inicio, "hora_fin", fin);
    }

    /** El horario de los datos de ejemplo: de lunes a viernes de 9:00 a 21:00 y el sábado de 10:00 a 14:00. */
    private static List<Map<String, Object>> semanaDeEjemplo() {
        List<Map<String, Object>> semana = new ArrayList<>();
        for (int d = 1; d <= 5; d++) semana.add(dia(d, "09:00", "21:00"));
        semana.add(dia(6, "10:00", "14:00"));
        return semana;
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

    /** Las horas que la landing ofrece ese día con ese odontólogo (sin sesión, como cualquier visitante). */
    private List<String> horasEnLaWeb(Tratamiento t, Odontologo o, LocalDate fecha) throws Exception {
        String cuerpo = mvc.perform(get("/api/disponibilidad").param("tratamiento_id", t.getId().toString())
                        .param("fecha", fecha.toString()).param("odontologo_id", o.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> horas = new ArrayList<>();
        leer(cuerpo).get("horarios").forEach(h -> horas.add(h.get("hora_inicio").asText()));
        return horas;
    }

    /** "ACCION usuario" de cada registro de auditoría de ese registro, en orden. */
    private List<String> auditoria(String entidad, long id) {
        return jdbc.queryForList("""
                select concat(a.accion, ' ', coalesce(u.username, '-')) from auditoria a
                left join usuarios u on u.id = a.usuario_id
                where a.entidad = ? and a.entidad_id = ? order by a.id""", String.class, entidad, String.valueOf(id));
    }
}
