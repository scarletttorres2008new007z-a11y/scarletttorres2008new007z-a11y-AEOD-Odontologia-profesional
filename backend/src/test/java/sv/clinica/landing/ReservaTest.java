package sv.clinica.landing;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import sv.clinica.landing.dto.CitaRequest;
import sv.clinica.landing.entity.Cita;
import sv.clinica.landing.entity.EstadoCita;
import sv.clinica.landing.entity.Franja;
import sv.clinica.landing.entity.ListaEspera;
import sv.clinica.landing.entity.Odontologo;
import sv.clinica.landing.entity.OrigenCita;
import sv.clinica.landing.entity.Tratamiento;
import sv.clinica.landing.exception.CitaNoModificableException;
import sv.clinica.landing.exception.HorarioNoDisponibleException;
import sv.clinica.landing.repository.CitaRepository;
import sv.clinica.landing.repository.ListaEsperaRepository;
import sv.clinica.landing.service.CitaService;
import sv.clinica.landing.service.DisponibilidadService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reserva, doble reserva, cancelación y reprogramación. Reloj fijo: lunes 12/10/2026 a las 08:00. */
class ReservaTest extends PruebaIntegracion {

    @Autowired
    CitaService citaService;
    @Autowired
    DisponibilidadService disponibilidad;
    @Autowired
    CitaRepository citas;
    @Autowired
    ListaEsperaRepository listaEspera;

    private List<String> horas(Tratamiento t, LocalDate fecha, Odontologo o) {
        return disponibilidad.buscar(t, fecha, o, null).stream().map(h -> h.horaInicio().toString()).toList();
    }

    private Cita reservar(Tratamiento t, Odontologo o, LocalDate fecha, int hora, int minuto) {
        return citaService.reservar(new CitaRequest(t.getId(), o.getId(), fecha, LocalTime.of(hora, minuto),
                "Lucía Gómez", "600123456", "lucia@example.com", null), OrigenCita.LANDING);
    }

    private int contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void reservaElHuecoYDejaDeOfrecerse() throws Exception {
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");

        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(limpieza, ana, MARTES, "10:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tu cita está reservada. Te llamaremos para confirmarla."))
                .andExpect(jsonPath("$.datos.codigo").isString())
                .andExpect(jsonPath("$.datos.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.datos.fecha").value(MARTES.toString()))
                .andExpect(jsonPath("$.datos.hora_inicio").value("10:00"))
                .andExpect(jsonPath("$.datos.hora_fin").value("11:00"))
                .andExpect(jsonPath("$.datos.tratamiento").value("Limpieza dental"))
                .andExpect(jsonPath("$.datos.odontologo").value("Dra. Ana Villar"))
                .andExpect(jsonPath("$.datos.id").doesNotExist());

        // Ana: 9–12 libre menos 10–11 → solo 09:00 y 11:00
        assertThat(horas(limpieza, MARTES, ana)).containsExactly("09:00", "11:00");
        assertThat(contar("select count(*) from agenda_ocupacion")).isEqualTo(4);
        assertThat(contar("select count(*) from notificaciones where tipo = 'NUEVA_CITA'")).isEqualTo(1);

        // El mismo horario otra vez → 409 con el mensaje para el paciente
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(limpieza, ana, MARTES, "10:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Este horario acaba de ser reservado. Selecciona otra opción."));
        // Un horario que se solapa (10:30) tampoco
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(tratamiento("Valoración odontológica"), ana, MARTES, "10:30")))
                .andExpect(status().isConflict());
        assertThat(contar("select count(*) from citas")).isEqualTo(1);
    }

    @Test
    void rechazaHorariosQueNoSeOfrecen() throws Exception {
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        // Almuerzo, fuera de turno, fuera de la rejilla y domingo
        for (String hora : List.of("12:00", "11:30", "14:00", "09:15")) {
            mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                            .content(citaJson(limpieza, ana, MARTES, hora)))
                    .andExpect(status().isConflict());
        }
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(limpieza, ana, DOMINGO, "10:00")))
                .andExpect(status().isConflict());
        // Hoy a las 08:30: dentro del margen mínimo de 60 minutos
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(limpieza, ana, LUNES, "08:30")))
                .andExpect(status().isConflict());
        // Ayer → 400 en el campo fecha
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(limpieza, ana, LUNES.minusDays(1), "10:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value("La fecha no puede ser anterior a hoy."));
        // El Dr. Marcos no hace limpiezas → 400 en odontologo_id
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(citaJson(limpieza, odontologo("Dr. Marcos Ortega"), MARTES, "14:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.odontologo_id").value("Este odontólogo no realiza el tratamiento seleccionado."));
        assertThat(contar("select count(*) from citas")).isZero();
    }

    /** Varias personas confirman el mismo horario a la vez: solo una lo consigue. */
    @RepeatedTest(3)
    void reservasSimultaneasDelMismoHueco() throws Exception {
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        List<Resultado> resultados = aLaVez(List.of(
                () -> reservar(limpieza, rocio, MIERCOLES, 15, 0),
                () -> reservar(limpieza, rocio, MIERCOLES, 15, 0),
                () -> reservar(limpieza, rocio, MIERCOLES, 15, 0),
                () -> reservar(limpieza, rocio, MIERCOLES, 15, 0)));

        assertThat(resultados).filteredOn(r -> r == Resultado.RESERVADA).hasSize(1);
        assertThat(resultados).filteredOn(r -> r == Resultado.OCUPADA).hasSize(3);
        assertThat(contar("select count(*) from citas")).isEqualTo(1);
        assertThat(contar("select count(*) from agenda_ocupacion")).isEqualTo(4);
    }

    /** Duraciones distintas que se pisan (60 min a las 10:00 y 30 min a las 10:30): solo entra una. */
    @RepeatedTest(3)
    void reservasSimultaneasQueSeSolapan() throws Exception {
        Odontologo ana = odontologo("Dra. Ana Villar");
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Tratamiento valoracion = tratamiento("Valoración odontológica");
        List<Resultado> resultados = aLaVez(List.of(
                () -> reservar(limpieza, ana, MIERCOLES, 10, 0),
                () -> reservar(valoracion, ana, MIERCOLES, 10, 30)));

        assertThat(resultados).containsOnlyOnce(Resultado.RESERVADA).containsOnlyOnce(Resultado.OCUPADA);
        assertThat(contar("select count(*) from citas")).isEqualTo(1);
    }

    @Test
    void cancelarNoBorraYLiberaElHueco() {
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Cita cita = reservar(limpieza, ana, MARTES, 10, 0);
        listaEspera.save(new ListaEspera(limpieza, null, MARTES, MARTES, Franja.MANANA,
                "Mañanas", "600000001", "manana@example.com", OrigenCita.LANDING, LocalDateTime.now()));
        listaEspera.save(new ListaEspera(limpieza, null, MARTES, MARTES, Franja.TARDE,
                "Tardes", "600000002", "tarde@example.com", OrigenCita.LANDING, LocalDateTime.now()));

        // Faltan más de 4 horas: el paciente puede cancelar
        Cita cancelada = citaService.cancelar(cita.getCodigo(), OrigenCita.LANDING, "No puedo ir");

        assertThat(cancelada.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cancelada.getCanceladaPor()).isEqualTo(OrigenCita.LANDING);
        assertThat(citas.findByCodigo(cita.getCodigo())).get()
                .extracting(Cita::getEstado).isEqualTo(EstadoCita.CANCELADA);
        assertThat(contar("select count(*) from agenda_ocupacion")).isZero();
        assertThat(horas(limpieza, MARTES, ana)).contains("10:00");
        assertThat(contar("select count(*) from notificaciones where tipo = 'CITA_CANCELADA'")).isEqualTo(1);
        // Solo avisa a quien esperaba por la mañana
        assertThat(jdbc.queryForList("""
                select l.nombre from notificaciones n join lista_espera l on l.id = n.lista_espera_id
                where n.tipo = 'HUECO_LIBERADO'""", String.class)).containsExactly("Mañanas");

        // Ya no está activa
        assertThatThrownBy(() -> citaService.cancelar(cita.getCodigo(), OrigenCita.CLINICA, null))
                .isInstanceOf(CitaNoModificableException.class)
                .hasMessage("Esta cita ya no está activa y no se puede cancelar.");
        // El hueco se puede volver a reservar
        assertThat(reservar(limpieza, ana, MARTES, 10, 0).getEstado()).isEqualTo(EstadoCita.PENDIENTE);
    }

    @Test
    void elPacienteNoCancelaConMenosDeCuatroHoras() {
        // Son las 08:00 y la cita es hoy a las 11:00
        Cita cita = reservar(tratamiento("Limpieza dental"), odontologo("Dra. Ana Villar"), LUNES, 11, 0);

        assertThat(citaService.puedeModificar(cita, OrigenCita.LANDING)).isFalse();
        assertThatThrownBy(() -> citaService.cancelar(cita.getCodigo(), OrigenCita.APP_PACIENTE, null))
                .isInstanceOf(CitaNoModificableException.class)
                .hasMessage("Solo se puede cancelar con al menos 4 horas de antelación. Llama a la clínica.");

        // La clínica sí puede
        assertThat(citaService.cancelar(cita.getCodigo(), OrigenCita.CLINICA, "Avisó por teléfono").getEstado())
                .isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void reprogramarMueveLaCitaYLiberaLaAnterior() {
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Odontologo rocio = odontologo("Dra. Rocío Fernández");
        Cita original = reservar(limpieza, ana, MARTES, 10, 0);

        Cita nueva = citaService.reprogramar(original.getCodigo(), rocio.getId(), MIERCOLES, LocalTime.of(15, 0),
                OrigenCita.LANDING);

        assertThat(nueva.getCodigo()).isNotEqualTo(original.getCodigo());
        assertThat(nueva.getHoraFin()).isEqualTo(LocalTime.of(16, 0));
        assertThat(citas.findByCodigo(original.getCodigo())).get()
                .extracting(Cita::getEstado).isEqualTo(EstadoCita.REPROGRAMADA);
        assertThat(contar("select count(*) from citas where cita_anterior_id = ?", original.getId())).isEqualTo(1);
        assertThat(horas(limpieza, MARTES, ana)).contains("10:00");
        assertThat(horas(limpieza, MIERCOLES, rocio)).doesNotContain("14:30", "15:00", "15:30");
        assertThat(contar("select count(*) from notificaciones where tipo = 'CITA_REPROGRAMADA'")).isEqualTo(1);
    }

    @Test
    void siElNuevoHorarioEstaOcupadoNoSePierdeLaCita() {
        Tratamiento limpieza = tratamiento("Limpieza dental");
        Odontologo ana = odontologo("Dra. Ana Villar");
        Cita original = reservar(limpieza, ana, MARTES, 10, 0);
        reservar(limpieza, ana, MARTES, 11, 0);

        assertThatThrownBy(() -> citaService.reprogramar(original.getCodigo(), ana.getId(), MARTES,
                LocalTime.of(11, 0), OrigenCita.LANDING))
                .isInstanceOf(HorarioNoDisponibleException.class);

        assertThat(citas.findByCodigo(original.getCodigo())).get()
                .extracting(Cita::getEstado).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(contar("select count(*) from agenda_ocupacion")).isEqualTo(8);
    }

    /* ─────────────── Concurrencia ─────────────── */

    enum Resultado { RESERVADA, OCUPADA }

    /** Lanza todas las reservas a la vez y devuelve cómo terminó cada una. */
    private List<Resultado> aLaVez(List<Callable<Cita>> reservas) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(reservas.size());
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Resultado>> futuros = reservas.stream().map(r -> hilos.submit(() -> {
                salida.await();
                try {
                    r.call();
                    return Resultado.RESERVADA;
                } catch (HorarioNoDisponibleException ocupada) {
                    return Resultado.OCUPADA;
                }
            })).toList();
            salida.countDown();
            List<Resultado> resultados = new java.util.ArrayList<>();
            for (Future<Resultado> f : futuros) resultados.add(f.get());
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }
}
