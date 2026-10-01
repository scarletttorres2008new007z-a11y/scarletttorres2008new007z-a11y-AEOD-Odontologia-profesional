package sv.clinica.landing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.landing.entity.Odontologo;
import sv.clinica.landing.entity.Tratamiento;
import sv.clinica.landing.repository.OdontologoRepository;
import sv.clinica.landing.repository.TratamientoRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de prueba SOLO para desarrollo: los mismos tratamientos, precios orientativos
 * y equipo que muestra hoy la landing de AEOD. Son de ejemplo (no verificados):
 * sustituirlos por los datos reales de la clínica antes de publicar.
 * Solo se insertan si la tabla está vacía. Se desactiva con app.datos-iniciales=false.
 */
@Component
@ConditionalOnProperty(name = "app.datos-iniciales", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String UNSPLASH = "https://images.unsplash.com/";
    private static final String FOTO_SERVICIO = "?auto=format&fit=crop&w=720&h=450&q=75";
    private static final String FOTO_EQUIPO = "?auto=format&fit=crop&w=600&h=800&q=75&crop=faces";

    private final TratamientoRepository tratamientos;
    private final OdontologoRepository odontologos;

    public DataInitializer(TratamientoRepository tratamientos, OdontologoRepository odontologos) {
        this.tratamientos = tratamientos;
        this.odontologos = odontologos;
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
    }

    private static List<Tratamiento> tratamientosIniciales() {
        String general = UNSPLASH + "photo-1609840114035-3c981b782dfe" + FOTO_SERVICIO;
        String ortodoncia = UNSPLASH + "photo-1606811971618-4486d14f3f99" + FOTO_SERVICIO;
        String implantes = UNSPLASH + "photo-1550831107-1553da8c8464" + FOTO_SERVICIO;
        return List.of(
                new Tratamiento("Valoración odontológica",
                        "Primera visita: exploración completa, diagnóstico y plan de tratamiento con presupuesto cerrado por escrito.",
                        "Primera revisión gratuita con diagnóstico y plan de tratamiento.",
                        new BigDecimal("0"), "30 min", general, 1),
                new Tratamiento("Limpieza dental",
                        "Higiene profesional para eliminar placa y sarro y prevenir la enfermedad periodontal.",
                        "Higiene profesional y revisión.",
                        new BigDecimal("60"), "45 min", general, 2),
                new Tratamiento("Empaste de composite",
                        "Restauración de caries con resina del color del diente. Precio por pieza.",
                        "Restauración estética de caries, por pieza.",
                        new BigDecimal("80"), "30–45 min", general, 3),
                new Tratamiento("Endodoncia",
                        "Tratamiento de conductos para conservar un diente con la pulpa dañada. Precio por pieza.",
                        "Tratamiento de conductos, por pieza.",
                        new BigDecimal("280"), "60–90 min", general, 4),
                new Tratamiento("Extracción simple",
                        "Extracción de una pieza dental sin complicaciones quirúrgicas. Precio por pieza.",
                        "Extracción sin complicaciones, por pieza.",
                        new BigDecimal("120"), "30 min", general, 5),
                new Tratamiento("Implante dental completo",
                        "Implante con corona para sustituir una pieza perdida. Precio por pieza.",
                        "Implante con corona, por pieza.",
                        new BigDecimal("990"), "Varias sesiones", implantes, 6),
                new Tratamiento("Invisalign Lite",
                        "Ortodoncia invisible con alineadores para casos leves.",
                        "Alineadores invisibles para casos leves.",
                        new BigDecimal("1800"), "Según el caso", ortodoncia, 7),
                new Tratamiento("Invisalign Full",
                        "Ortodoncia invisible con alineadores para casos completos.",
                        "Alineadores invisibles para casos completos.",
                        new BigDecimal("3200"), "Según el caso", ortodoncia, 8),
                new Tratamiento("Brackets metálicos",
                        "Ortodoncia fija con brackets metálicos.",
                        "Ortodoncia fija convencional.",
                        new BigDecimal("2200"), "Según el caso", ortodoncia, 9),
                new Tratamiento("Carilla de porcelana",
                        "Lámina de porcelana que mejora forma y color del diente. Precio por pieza.",
                        "Estética dental, por pieza.",
                        new BigDecimal("390"), "2–3 sesiones", implantes, 10),
                new Tratamiento("Blanqueamiento LED",
                        "Blanqueamiento dental en consulta con lámpara LED.",
                        "Blanqueamiento dental en consulta.",
                        new BigDecimal("250"), "60–90 min", implantes, 11));
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
