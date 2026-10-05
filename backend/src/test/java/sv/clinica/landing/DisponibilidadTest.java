package sv.clinica.landing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.landing.entity.Bloqueo;
import sv.clinica.landing.entity.Franja;
import sv.clinica.landing.entity.HorarioClinica;
import sv.clinica.landing.entity.HorarioOdontologo;
import sv.clinica.landing.entity.Odontologo;
import sv.clinica.landing.entity.OrigenCita;
import sv.clinica.landing.entity.TipoBloqueo;
import sv.clinica.landing.entity.Tratamiento;
import sv.clinica.landing.dto.CitaRequest;
import sv.clinica.landing.repository.BloqueoRepository;
import sv.clinica.landing.repository.HorarioClinicaRepository;
import sv.clinica.landing.repository.HorarioOdontologoRepository;
import sv.clinica.landing.service.CitaService;
import sv.clinica.landing.service.DisponibilidadService;
import sv.clinica.landing.service.DisponibilidadService.Horario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cálculo de disponibilidad con la agenda de ejemplo:
 * clínica L–V 9–21, almuerzo 12–13; Ana 9–13, Rocío 9–17, Carlos 11–17, Marcos 13–21.
 */
class DisponibilidadTest extends PruebaIntegracion {

    @Autowired
    DisponibilidadService disponibilidad;
    @Autowired
    CitaService citaService;
    @Autowired
    HorarioClinicaRepository horariosClinica;
    @Autowired
    HorarioOdontologoRepository horariosOdontologo;
    @Autowired
    BloqueoRepository bloqueos;

    private static List<String> horas(List<Horario> horarios) {
        return horarios.stream().map(h -> h.horaInicio().toString()).toList();
    }

    /** El ejemplo del enunciado: 8–17, almuerzo 12–13, cita 10–11, limpieza de 60 min. */
    @Test
    @Transactional
    void ejemploDelEnunciado() {
        LocalDate dia = MIERCOLES;
        horariosClinica.deleteAllInBatch();
        horariosClinica.save(new HorarioClinica(3, LocalTime.of(8, 0), LocalTime.of(17, 0)));
        Odontologo doctora = odontologos.save(new Odontologo("Dra. Ejemplo", "General", null, null, 9));
        horariosOdontologo.save(new HorarioOdontologo(doctora, 3, LocalTime.of(8, 0), LocalTime.of(17, 0)));
        Tratamiento limpieza = tratamientos.save(
                new Tratamiento("Limpieza de ejemplo", null, null, BigDecimal.TEN, "60 min", 60, null, 99));
        limpieza.getOdontologos().add(doctora);
        citaService.reservar(new CitaRequest(limpieza.getId(), doctora.getId(), dia, LocalTime.of(10, 0),
                "Paciente Uno", "600123456", "uno@example.com", null), OrigenCita.LANDING);

        List<Horario> horarios = disponibilidad.buscar(limpieza, dia, null, null);

        assertThat(horas(horarios)).containsExactly(
                "08:00", "08:30", "09:00", "11:00", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00");
        assertThat(horas(horarios)).doesNotContain("10:00", "12:00", "11:30", "16:30");
    }

    @Test
    void limpiezaConCualquierOdontologoRespetaTurnosYAlmuerzo() {
        List<Horario> horarios = disponibilidad.buscar(tratamiento("Limpieza dental"), MARTES, null, null);
        assertThat(horas(horarios)).containsExactly(
                "09:00", "09:30", "10:00", "10:30", "11:00",
                "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00");
        // Por la tarde solo trabaja la Dra. Rocío (Ana sale a las 13:00)
        assertThat(horarios.stream().filter(h -> h.horaInicio().getHour() >= 13))
                .allMatch(h -> h.odontologo().getNombre().equals("Dra. Rocío Fernández"));
    }

    @Test
    void filtraPorOdontologoYFranja() {
        Odontologo ana = odontologo("Dra. Ana Villar");
        assertThat(horas(disponibilidad.buscar(tratamiento("Limpieza dental"), MARTES, ana, null)))
                .containsExactly("09:00", "09:30", "10:00", "10:30", "11:00");
        assertThat(horas(disponibilidad.buscar(tratamiento("Limpieza dental"), MARTES, null, Franja.TARDE)))
                .containsExactly("14:00", "14:30", "15:00", "15:30", "16:00");
    }

    /** 90 minutos seguidos: el hueco de 11–12 de Carlos no sirve, aunque haya más tiempo libre ese día. */
    @Test
    void necesitaUnBloqueContinuo() {
        assertThat(horas(disponibilidad.buscar(tratamiento("Blanqueamiento LED"), MARTES, null, null)))
                .containsExactly("13:00", "13:30", "14:00", "14:30", "15:00", "15:30");
    }

    @Test
    void hoyRespetaLaAntelacionMinima() {
        // Son las 08:00: con 60 min de antelación, lo primero es a las 09:00
        assertThat(horas(disponibilidad.buscar(tratamiento("Limpieza dental"), LUNES, null, null))).startsWith("09:00");
    }

    @Test
    @Transactional
    void feriadoYVacacionesBloqueanElDia() {
        bloqueos.save(new Bloqueo(TipoBloqueo.FERIADO, "Feriado", null, MARTES, MARTES, null, null, null));
        assertThat(disponibilidad.buscar(tratamiento("Limpieza dental"), MARTES, null, null)).isEmpty();

        bloqueos.save(new Bloqueo(TipoBloqueo.VACACIONES, "Vacaciones", odontologo("Dra. Rocío Fernández"),
                MIERCOLES, MIERCOLES, null, null, null));
        assertThat(disponibilidad.buscar(tratamiento("Limpieza dental"), MIERCOLES, null, null))
                .isNotEmpty()
                .allMatch(h -> h.odontologo().getNombre().equals("Dra. Ana Villar"));
    }

    @Test
    void domingoSinHuecosProponeProximasOpciones() throws Exception {
        mvc.perform(get("/api/disponibilidad")
                        .param("tratamiento_id", tratamiento("Limpieza dental").getId().toString())
                        .param("fecha", DOMINGO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios", hasSize(0)))
                .andExpect(jsonPath("$.mensaje").value("No encontramos disponibilidad para el horario seleccionado."))
                .andExpect(jsonPath("$.proximas_opciones", hasSize(6)))
                .andExpect(jsonPath("$.proximas_opciones[0].fecha").value(DOMINGO.plusDays(1).toString()))
                .andExpect(jsonPath("$.proximas_opciones[0].hora_inicio").value("09:00"))
                .andExpect(jsonPath("$.proximas_opciones[1].hora_inicio").value("10:00"));
    }

    @Test
    void respuestaDelDia() throws Exception {
        mvc.perform(get("/api/disponibilidad")
                        .param("tratamiento_id", tratamiento("Limpieza dental").getId().toString())
                        .param("fecha", MARTES.toString())
                        .param("franja", "MANANA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tratamiento.duracion_minutos").value(60))
                .andExpect(jsonPath("$.horarios", hasSize(7)))
                .andExpect(jsonPath("$.horarios[0].hora_inicio").value("09:00"))
                .andExpect(jsonPath("$.horarios[0].hora_fin").value("10:00"))
                .andExpect(jsonPath("$.horarios[0].odontologo_nombre").exists())
                .andExpect(jsonPath("$.proximas_opciones").doesNotExist());
    }
}
