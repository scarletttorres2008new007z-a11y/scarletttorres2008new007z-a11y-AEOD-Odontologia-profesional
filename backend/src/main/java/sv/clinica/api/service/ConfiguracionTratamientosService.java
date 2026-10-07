package sv.clinica.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.TratamientoConfigRequest;
import sv.clinica.api.dto.TratamientoConfigResponse;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.exception.ConflictoException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.repository.TratamientoRepository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tratamientos desde el software: alta, duración de la cita, precio orientativo, quién los hace y si se ofrecen.
 * La duración es la que usa el backend para calcular los huecos libres, en la web y en el software. Cambiarla no
 * toca las citas ya dadas: solo las nuevas.
 */
@Service
public class ConfiguracionTratamientosService {

    private static final int PASO_MINUTOS = 15;

    private final TratamientoRepository tratamientos;
    private final OdontologoRepository odontologos;
    private final CitasAfectadas citasAfectadas;
    private final AuditoriaService auditoria;

    public ConfiguracionTratamientosService(TratamientoRepository tratamientos, OdontologoRepository odontologos,
                                            CitasAfectadas citasAfectadas, AuditoriaService auditoria) {
        this.tratamientos = tratamientos;
        this.odontologos = odontologos;
        this.citasAfectadas = citasAfectadas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<TratamientoConfigResponse> listar() {
        return tratamientos.findAllByOrderByOrdenAscIdAsc().stream().map(TratamientoConfigResponse::from).toList();
    }

    @Transactional
    public TratamientoConfigResponse crear(TratamientoConfigRequest request) {
        String nombre = request.nombre().trim();
        comprobarNombreLibre(nombre, null);
        Tratamiento tratamiento = new Tratamiento(nombre, null, Textos.opcional(request.descripcionCorta()),
                request.precioDesde(), Textos.opcional(request.duracionAproximada()), duracion(request), null,
                tratamientos.ultimoOrden() + 1);
        tratamiento.cambiarOdontologos(quienLoHace(request.odontologoIds()));
        tratamientos.save(tratamiento);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CREAR, EntidadAuditoria.TRATAMIENTO,
                tratamiento.getId(), null, datosAuditables(tratamiento));
        return TratamientoConfigResponse.from(tratamiento);
    }

    @Transactional
    public TratamientoConfigResponse editar(Long id, TratamientoConfigRequest request) {
        Tratamiento tratamiento = buscar(id);
        String nombre = request.nombre().trim();
        comprobarNombreLibre(nombre, id);
        Map<String, Object> antes = datosAuditables(tratamiento);
        tratamiento.editar(nombre, Textos.opcional(request.descripcionCorta()), request.precioDesde(),
                Textos.opcional(request.duracionAproximada()), duracion(request));
        tratamiento.cambiarOdontologos(quienLoHace(request.odontologoIds()));
        Map<String, Object> despues = datosAuditables(tratamiento);
        // En la auditoría quedan solo los datos que cambian, con su valor de antes y el de después
        antes.keySet().removeIf(clave -> Objects.equals(antes.get(clave), despues.get(clave)));
        despues.keySet().retainAll(antes.keySet());
        if (!antes.isEmpty()) {
            auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.EDITAR, EntidadAuditoria.TRATAMIENTO, id,
                    antes, despues);
        }
        return TratamientoConfigResponse.from(tratamiento);
    }

    /**
     * Desactivarlo lo quita de la web y de las citas nuevas. Si tiene citas pendientes o confirmadas de hoy en
     * adelante, no se deja: primero hay que atenderlas, moverlas o cancelarlas.
     */
    @Transactional
    public TratamientoConfigResponse cambiarEstado(Long id, boolean activo) {
        Tratamiento tratamiento = buscar(id);
        if (tratamiento.isActivo() != activo) {
            if (!activo) {
                List<Cita> pendientes = citasAfectadas.buscar(c -> c.getTratamiento().getId().equals(id));
                if (!pendientes.isEmpty()) {
                    throw new ConflictoException(Textos.citasPendientes(pendientes.size())
                            + " Espera a que pasen, o muévelas o cancélalas, antes de dejar de ofrecerlo.");
                }
            }
            tratamiento.setActivo(activo);
            auditoria.registrar(OrigenAuditoria.SOFTWARE,
                    activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR, EntidadAuditoria.TRATAMIENTO, id,
                    Map.of("activo", !activo), Map.of("activo", activo));
        }
        return TratamientoConfigResponse.from(tratamiento);
    }

    private Tratamiento buscar(Long id) {
        return tratamientos.findConOdontologosById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un tratamiento con id " + id + "."));
    }

    private void comprobarNombreLibre(String nombre, Long id) {
        tratamientos.findByNombre(nombre).filter(otro -> !otro.getId().equals(id)).ifPresent(otro -> {
            throw new ConflictoException("nombre", "Ya hay un tratamiento con ese nombre.");
        });
    }

    /** La agenda trabaja en cuartos de hora: la cita dura 15, 30, 45, 60… minutos. */
    private static int duracion(TratamientoConfigRequest request) {
        int minutos = request.duracionMinutos();
        if (minutos % PASO_MINUTOS != 0) {
            throw new DatosInvalidosException("duracion_minutos",
                    "La duración va de 15 en 15 minutos (15, 30, 45, 60…).");
        }
        return minutos;
    }

    /** Los odontólogos elegidos (vacío = cualquiera). Pueden estar desactivados si ya lo hacían antes. */
    private Set<Odontologo> quienLoHace(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        Set<Long> distintos = new LinkedHashSet<>(ids);
        List<Odontologo> encontrados = odontologos.findAllById(distintos);
        if (encontrados.size() != distintos.size()) {
            throw new DatosInvalidosException("odontologo_ids", "Alguno de los odontólogos elegidos no existe.");
        }
        return new LinkedHashSet<>(encontrados);
    }

    private static Map<String, Object> datosAuditables(Tratamiento t) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", t.getNombre());
        datos.put("descripcion_corta", t.getDescripcionCorta());
        datos.put("precio_desde", precio(t.getPrecioDesde()));
        datos.put("duracion_aproximada", t.getDuracionAproximada());
        datos.put("duracion_minutos", t.getDuracionMinutos());
        datos.put("odontologos", t.getOdontologos().isEmpty() ? "Cualquiera"
                : String.join(", ", t.getOdontologos().stream().map(Odontologo::getNombre).sorted().toList()));
        datos.put("activo", t.isActivo());
        return datos;
    }

    /** 49.00 y 49 son el mismo precio: se guarda sin ceros de más para comparar antes y después. */
    private static String precio(BigDecimal precio) {
        return precio == null ? null : precio.stripTrailingZeros().toPlainString();
    }
}
