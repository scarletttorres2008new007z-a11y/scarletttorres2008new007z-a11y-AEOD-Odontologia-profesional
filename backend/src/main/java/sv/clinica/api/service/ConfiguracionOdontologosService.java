package sv.clinica.api.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.OdontologoConfigRequest;
import sv.clinica.api.dto.OdontologoConfigResponse;
import sv.clinica.api.dto.UsuarioParaOdontologoResponse;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.exception.ConflictoException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.repository.TratamientoRepository;
import sv.clinica.api.repository.UsuarioRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Odontólogos desde el software: alta, datos que enseña la landing, activar o desactivar y vincularlos a su usuario
 * (con el que ve su propia agenda). Nada se borra: un odontólogo desactivado deja de salir en la web y en la agenda.
 */
@Service
public class ConfiguracionOdontologosService {

    private static final String CAMPO_USUARIO = "usuario_id";

    private final OdontologoRepository odontologos;
    private final TratamientoRepository tratamientos;
    private final UsuarioRepository usuarios;
    private final CitasAfectadas citasAfectadas;
    private final AuditoriaService auditoria;

    public ConfiguracionOdontologosService(OdontologoRepository odontologos, TratamientoRepository tratamientos,
                                           UsuarioRepository usuarios, CitasAfectadas citasAfectadas,
                                           AuditoriaService auditoria) {
        this.odontologos = odontologos;
        this.tratamientos = tratamientos;
        this.usuarios = usuarios;
        this.citasAfectadas = citasAfectadas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<OdontologoConfigResponse> listar() {
        List<Tratamiento> activos = tratamientos.findByActivoTrueOrderByOrdenAscIdAsc();
        return odontologos.findAllByOrderByOrdenAscIdAsc().stream().map(o -> respuesta(o, activos)).toList();
    }

    @Transactional
    public OdontologoConfigResponse crear(OdontologoConfigRequest request) {
        String nombre = request.nombre().trim();
        comprobarNombreLibre(nombre, null);
        Odontologo odontologo = new Odontologo(nombre, Textos.opcional(request.especialidad()),
                Textos.opcional(request.descripcion()), null, odontologos.ultimoOrden() + 1);
        odontologo.setUsuario(usuarioParaVincular(request.usuarioId(), null));
        guardar(odontologo);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CREAR, EntidadAuditoria.ODONTOLOGO,
                odontologo.getId(), null, datosAuditables(odontologo));
        return respuesta(odontologo, tratamientos.findByActivoTrueOrderByOrdenAscIdAsc());
    }

    @Transactional
    public OdontologoConfigResponse editar(Long id, OdontologoConfigRequest request) {
        Odontologo odontologo = buscar(id);
        String nombre = request.nombre().trim();
        comprobarNombreLibre(nombre, id);
        Map<String, Object> antes = datosAuditables(odontologo);
        odontologo.editar(nombre, Textos.opcional(request.especialidad()), Textos.opcional(request.descripcion()));
        odontologo.setUsuario(usuarioParaVincular(request.usuarioId(), odontologo));
        guardar(odontologo);
        registrarCambios(AccionAuditoria.EDITAR, id, antes, datosAuditables(odontologo));
        return respuesta(odontologo, tratamientos.findByActivoTrueOrderByOrdenAscIdAsc());
    }

    /**
     * Desactivarlo lo quita de la web, de la agenda y de los huecos libres. Si tiene citas pendientes o confirmadas
     * de hoy en adelante, no se deja: primero hay que moverlas a otro odontólogo o cancelarlas, para no perderlas.
     */
    @Transactional
    public OdontologoConfigResponse cambiarEstado(Long id, boolean activo) {
        Odontologo odontologo = buscar(id);
        if (odontologo.isActivo() != activo) {
            if (!activo) {
                List<Cita> pendientes = citasAfectadas.buscar(c -> c.getOdontologo().equals(odontologo));
                if (!pendientes.isEmpty()) {
                    throw new ConflictoException(Textos.citasPendientes(pendientes.size())
                            + " Muévelas a otro odontólogo o cancélalas antes de desactivarlo.");
                }
            }
            odontologo.setActivo(activo);
            auditoria.registrar(OrigenAuditoria.SOFTWARE,
                    activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR, EntidadAuditoria.ODONTOLOGO, id,
                    Map.of("activo", !activo), Map.of("activo", activo));
        }
        return respuesta(odontologo, tratamientos.findByActivoTrueOrderByOrdenAscIdAsc());
    }

    /** Usuarios activos que se pueden vincular, con el odontólogo al que ya está unido cada uno. */
    @Transactional(readOnly = true)
    public List<UsuarioParaOdontologoResponse> usuariosParaVincular() {
        Map<Long, Odontologo> porUsuario = odontologos.findAll().stream()
                .filter(o -> o.getUsuario() != null)
                .collect(Collectors.toMap(o -> o.getUsuario().getId(), Function.identity()));
        return usuarios.findAll(Sort.by("nombre", "id")).stream()
                .filter(Usuario::isActivo)
                .map(u -> UsuarioParaOdontologoResponse.from(u, porUsuario.get(u.getId())))
                .toList();
    }

    private Odontologo buscar(Long id) {
        return odontologos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un odontólogo con id " + id + "."));
    }

    private void comprobarNombreLibre(String nombre, Long id) {
        odontologos.findByNombre(nombre).filter(otro -> !otro.getId().equals(id)).ifPresent(otro -> {
            throw new ConflictoException("nombre", "Ya hay un odontólogo con ese nombre.");
        });
    }

    /**
     * El usuario que se vincula: activo y sin otro odontólogo. Si no cambia, se conserva aunque se haya desactivado
     * (así se pueden cambiar los demás datos sin tocar el vínculo).
     */
    private Usuario usuarioParaVincular(Long usuarioId, Odontologo odontologo) {
        if (usuarioId == null) return null;
        Usuario actual = odontologo == null ? null : odontologo.getUsuario();
        if (actual != null && actual.getId().equals(usuarioId)) return actual;
        Usuario usuario = usuarios.findById(usuarioId).filter(Usuario::isActivo)
                .orElseThrow(() -> new DatosInvalidosException(CAMPO_USUARIO, "Ese usuario no existe o está desactivado."));
        odontologos.findByUsuarioId(usuarioId).filter(otro -> !otro.equals(odontologo)).ifPresent(otro -> {
            throw new ConflictoException(CAMPO_USUARIO, "Ese usuario ya está vinculado a " + otro.getNombre() + ".");
        });
        return usuario;
    }

    private void guardar(Odontologo odontologo) {
        try {
            odontologos.saveAndFlush(odontologo);
        } catch (DataIntegrityViolationException e) {
            // Dos personas vincularon el mismo usuario a la vez: la base de datos solo acepta un vínculo
            throw new ConflictoException(CAMPO_USUARIO, "Ese usuario ya está vinculado a otro odontólogo.");
        }
    }

    private void registrarCambios(AccionAuditoria accion, Long id, Map<String, Object> antes, Map<String, Object> despues) {
        // En la auditoría quedan solo los datos que cambian, con su valor de antes y el de después
        antes.keySet().removeIf(clave -> Objects.equals(antes.get(clave), despues.get(clave)));
        despues.keySet().retainAll(antes.keySet());
        if (antes.isEmpty()) return;
        auditoria.registrar(OrigenAuditoria.SOFTWARE, accion, EntidadAuditoria.ODONTOLOGO, id, antes, despues);
    }

    private static Map<String, Object> datosAuditables(Odontologo o) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", o.getNombre());
        datos.put("especialidad", o.getEspecialidad());
        datos.put("descripcion", o.getDescripcion());
        datos.put("usuario", o.getUsuario() == null ? null
                : o.getUsuario().getNombre() + " (" + o.getUsuario().getUsername() + ")");
        datos.put("activo", o.isActivo());
        return datos;
    }

    private static OdontologoConfigResponse respuesta(Odontologo o, List<Tratamiento> activos) {
        return OdontologoConfigResponse.from(o,
                activos.stream().filter(t -> t.loRealiza(o)).map(Tratamiento::getNombre).toList());
    }
}
