package sv.clinica.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.entity.Bloqueo;
import sv.clinica.api.entity.HorarioClinica;
import sv.clinica.api.entity.HorarioOdontologo;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.TipoBloqueo;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.repository.BloqueoRepository;
import sv.clinica.api.repository.HorarioClinicaRepository;
import sv.clinica.api.repository.HorarioOdontologoRepository;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.repository.TratamientoRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datos de prueba SOLO para desarrollo: los mismos tratamientos, precios orientativos
 * y equipo que muestra hoy la landing de AEOD, más una agenda de ejemplo
 * (horario de la clínica, turnos, almuerzo, duraciones y quién hace cada tratamiento).
 * Son de ejemplo (no verificados): sustituirlos por los datos reales antes de publicar.
 *
 * Cada bloque solo se inserta si su tabla está vacía, así una base de datos de la versión
 * anterior se completa sola al arrancar. Se desactiva con app.datos-iniciales=false.
 */
@Component
@ConditionalOnProperty(name = "app.datos-iniciales", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String UNSPLASH = "https://images.unsplash.com/";
    private static final String FOTO_SERVICIO = "?auto=format&fit=crop&w=720&h=450&q=75";
    private static final String FOTO_EQUIPO = "?auto=format&fit=crop&w=600&h=800&q=75&crop=faces";

    /** Minutos de la cita online de cada tratamiento (también rellena bases de datos anteriores). */
    private static final Map<String, Integer> DURACIONES = Map.ofEntries(
            Map.entry("Valoración odontológica", 30),
            Map.entry("Limpieza dental", 60),
            Map.entry("Empaste de composite", 45),
            Map.entry("Endodoncia", 90),
            Map.entry("Extracción simple", 30),
            Map.entry("Implante dental completo", 120),
            Map.entry("Invisalign Lite", 60),
            Map.entry("Invisalign Full", 60),
            Map.entry("Brackets metálicos", 60),
            Map.entry("Carilla de porcelana", 60),
            Map.entry("Blanqueamiento LED", 90));

    private static final String ROCIO = "Dra. Rocío Fernández";
    private static final String MARCOS = "Dr. Marcos Ortega";
    private static final String ANA = "Dra. Ana Villar";
    private static final String CARLOS = "Dr. Carlos Méndez";

    /** Quién hace cada tratamiento. Los que no aparecen (valoración) los hace cualquiera. */
    private static final Map<String, List<String>> QUIEN_LO_HACE = Map.ofEntries(
            Map.entry("Limpieza dental", List.of(ANA, ROCIO)),
            Map.entry("Empaste de composite", List.of(ANA, ROCIO, CARLOS)),
            Map.entry("Endodoncia", List.of(ROCIO, ANA)),
            Map.entry("Extracción simple", List.of(ANA, ROCIO)),
            Map.entry("Implante dental completo", List.of(ROCIO)),
            Map.entry("Invisalign Lite", List.of(MARCOS)),
            Map.entry("Invisalign Full", List.of(MARCOS)),
            Map.entry("Brackets metálicos", List.of(MARCOS)),
            Map.entry("Carilla de porcelana", List.of(CARLOS)),
            Map.entry("Blanqueamiento LED", List.of(CARLOS)));

    private static final List<Integer> LUNES_A_VIERNES = List.of(1, 2, 3, 4, 5);
    private static final int SABADO = 6;

    private final TratamientoRepository tratamientos;
    private final OdontologoRepository odontologos;
    private final HorarioClinicaRepository horariosClinica;
    private final HorarioOdontologoRepository horariosOdontologo;
    private final BloqueoRepository bloqueos;

    public DataInitializer(TratamientoRepository tratamientos, OdontologoRepository odontologos,
                           HorarioClinicaRepository horariosClinica, HorarioOdontologoRepository horariosOdontologo,
                           BloqueoRepository bloqueos) {
        this.tratamientos = tratamientos;
        this.odontologos = odontologos;
        this.horariosClinica = horariosClinica;
        this.horariosOdontologo = horariosOdontologo;
        this.bloqueos = bloqueos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (tratamientos.count() == 0) {
            tratamientos.saveAll(tratamientosIniciales());
            log.info("Datos de prueba: tratamientos insertados.");
        }
        if (odontologos.count() == 0) {
            odontologos.saveAll(odontologosIniciales());
            log.info("Datos de prueba: odontólogos insertados.");
        }
        completarDuraciones();
        if (!tratamientos.existsByOdontologosIsNotEmpty()) {
            asignarOdontologos();
        }
        if (horariosClinica.count() == 0) {
            horariosClinica.saveAll(horarioClinica());
            log.info("Datos de prueba: horario de la clínica insertado.");
        }
        if (horariosOdontologo.count() == 0) {
            horariosOdontologo.saveAll(turnosOdontologos());
            log.info("Datos de prueba: turnos de los odontólogos insertados.");
        }
        if (bloqueos.count() == 0) {
            bloqueos.saveAll(almuerzo());
            log.info("Datos de prueba: almuerzo insertado.");
        }
    }

    /* ── Agenda de ejemplo ── */

    /** El mismo horario que muestra la landing: L–V 9:00–21:00, S 10:00–14:00, domingo cerrado. */
    private static List<HorarioClinica> horarioClinica() {
        List<HorarioClinica> dias = new ArrayList<>();
        LUNES_A_VIERNES.forEach(dia -> dias.add(new HorarioClinica(dia, LocalTime.of(9, 0), LocalTime.of(21, 0))));
        dias.add(new HorarioClinica(SABADO, LocalTime.of(10, 0), LocalTime.of(14, 0)));
        return dias;
    }

    private List<HorarioOdontologo> turnosOdontologos() {
        List<HorarioOdontologo> turnos = new ArrayList<>();
        turno(turnos, ROCIO, LUNES_A_VIERNES, 9, 17);
        turno(turnos, MARCOS, LUNES_A_VIERNES, 13, 21);
        turno(turnos, ANA, LUNES_A_VIERNES, 9, 13);
        turno(turnos, ANA, List.of(SABADO), 10, 14);
        turno(turnos, CARLOS, LUNES_A_VIERNES, 11, 17);
        turno(turnos, CARLOS, List.of(SABADO), 10, 14);
        return turnos;
    }

    private void turno(List<HorarioOdontologo> turnos, String nombre, List<Integer> dias, int desde, int hasta) {
        odontologos.findByNombre(nombre).ifPresent(o -> dias.forEach(dia ->
                turnos.add(new HorarioOdontologo(o, dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0)))));
    }

    /** Almuerzo de lunes a viernes, 12:00–13:00, para toda la clínica. */
    private static List<Bloqueo> almuerzo() {
        return LUNES_A_VIERNES.stream()
                .map(dia -> Bloqueo.semanal(TipoBloqueo.ALMUERZO, "Almuerzo", dia, LocalTime.of(12, 0), LocalTime.of(13, 0)))
                .toList();
    }

    private void completarDuraciones() {
        for (Tratamiento t : tratamientos.findAll()) {
            if (t.getDuracionMinutos() == null) {
                t.setDuracionMinutos(DURACIONES.getOrDefault(t.getNombre(), 30));
            }
        }
    }

    private void asignarOdontologos() {
        QUIEN_LO_HACE.forEach((tratamiento, nombres) -> tratamientos.findByNombre(tratamiento).ifPresent(t ->
                nombres.forEach(nombre -> odontologos.findByNombre(nombre).ifPresent(t.getOdontologos()::add))));
        log.info("Datos de prueba: odontólogos asignados a cada tratamiento.");
    }

    private static List<Tratamiento> tratamientosIniciales() {
        String general = UNSPLASH + "photo-1609840114035-3c981b782dfe" + FOTO_SERVICIO;
        String ortodoncia = UNSPLASH + "photo-1606811971618-4486d14f3f99" + FOTO_SERVICIO;
        String implantes = UNSPLASH + "photo-1550831107-1553da8c8464" + FOTO_SERVICIO;
        return List.of(
                new Tratamiento("Valoración odontológica",
                        "Primera visita: exploración completa, diagnóstico y plan de tratamiento con presupuesto cerrado por escrito.",
                        "Primera revisión gratuita con diagnóstico y plan de tratamiento.",
                        new BigDecimal("0"), "30 min", 30, general, 1),
                new Tratamiento("Limpieza dental",
                        "Higiene profesional para eliminar placa y sarro y prevenir la enfermedad periodontal.",
                        "Higiene profesional y revisión.",
                        new BigDecimal("60"), "60 min", 60, general, 2),
                new Tratamiento("Empaste de composite",
                        "Restauración de caries con resina del color del diente. Precio por pieza.",
                        "Restauración estética de caries, por pieza.",
                        new BigDecimal("80"), "30–45 min", 45, general, 3),
                new Tratamiento("Endodoncia",
                        "Tratamiento de conductos para conservar un diente con la pulpa dañada. Precio por pieza.",
                        "Tratamiento de conductos, por pieza.",
                        new BigDecimal("280"), "60–90 min", 90, general, 4),
                new Tratamiento("Extracción simple",
                        "Extracción de una pieza dental sin complicaciones quirúrgicas. Precio por pieza.",
                        "Extracción sin complicaciones, por pieza.",
                        new BigDecimal("120"), "30 min", 30, general, 5),
                new Tratamiento("Implante dental completo",
                        "Implante con corona para sustituir una pieza perdida. Precio por pieza.",
                        "Implante con corona, por pieza.",
                        new BigDecimal("990"), "Varias sesiones", 120, implantes, 6),
                new Tratamiento("Invisalign Lite",
                        "Ortodoncia invisible con alineadores para casos leves.",
                        "Alineadores invisibles para casos leves.",
                        new BigDecimal("1800"), "Según el caso", 60, ortodoncia, 7),
                new Tratamiento("Invisalign Full",
                        "Ortodoncia invisible con alineadores para casos completos.",
                        "Alineadores invisibles para casos completos.",
                        new BigDecimal("3200"), "Según el caso", 60, ortodoncia, 8),
                new Tratamiento("Brackets metálicos",
                        "Ortodoncia fija con brackets metálicos.",
                        "Ortodoncia fija convencional.",
                        new BigDecimal("2200"), "Según el caso", 60, ortodoncia, 9),
                new Tratamiento("Carilla de porcelana",
                        "Lámina de porcelana que mejora forma y color del diente. Precio por pieza.",
                        "Estética dental, por pieza.",
                        new BigDecimal("390"), "2–3 sesiones", 60, implantes, 10),
                new Tratamiento("Blanqueamiento LED",
                        "Blanqueamiento dental en consulta con lámpara LED.",
                        "Blanqueamiento dental en consulta.",
                        new BigDecimal("250"), "60–90 min", 90, implantes, 11));
    }

    private static List<Odontologo> odontologosIniciales() {
        return List.of(
                new Odontologo("Dra. Rocío Fernández", "Directora clínica · Implantología",
                        "Más de 3.200 implantes colocados con éxito documentado.",
                        UNSPLASH + "photo-1758691462651-611d730c5272" + FOTO_EQUIPO, 1),
                new Odontologo("Dr. Marcos Ortega", "Ortodoncia",
                        "Especialista en ortodoncia invisible, con más de 800 casos tratados.",
                        UNSPLASH + "photo-1612349317150-e413f6a5b16d" + FOTO_EQUIPO, 2),
                new Odontologo("Dra. Ana Villar", "Periodoncia · Cirugía oral",
                        "Especialista en enfermedad periodontal y regeneración ósea guiada.",
                        UNSPLASH + "photo-1678695972687-033fa0bdbac9" + FOTO_EQUIPO, 3),
                new Odontologo("Dr. Carlos Méndez", "Estética dental · DSD",
                        "Carillas de porcelana y estética mínimamente invasiva. Más de 500 diseños de sonrisa.",
                        UNSPLASH + "photo-1622253692010-333f2da6031d" + FOTO_EQUIPO, 4));
    }
}
