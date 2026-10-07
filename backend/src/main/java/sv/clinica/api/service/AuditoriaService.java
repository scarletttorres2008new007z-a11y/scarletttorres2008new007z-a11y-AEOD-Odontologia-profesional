package sv.clinica.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sv.clinica.api.dto.AuditoriaResponse;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.dto.UsuarioResumen;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.RegistroAuditoria;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.repository.AuditoriaRepository;
import sv.clinica.api.repository.UsuarioRepository;
import sv.clinica.api.security.UsuarioAutenticado;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de auditoría: quién hizo qué, desde dónde, sobre qué registro y cómo estaba antes y después.
 * Se guarda en la misma transacción que la operación: si la operación se deshace, su registro también.
 * Nunca se guardan contraseñas ni sus hashes.
 */
@Service
public class AuditoriaService {

    private static final int LONGITUD_IP = 45;

    private final AuditoriaRepository registros;
    private final UsuarioRepository usuarios;
    private final ObjectMapper json;
    private final Clock clock;

    public AuditoriaService(AuditoriaRepository registros, UsuarioRepository usuarios, ObjectMapper json, Clock clock) {
        this.registros = registros;
        this.usuarios = usuarios;
        this.json = json;
        this.clock = clock;
    }

    /** Registra una operación de quien hace la petición (sin usuario si no hay sesión, por ejemplo desde la landing). */
    public void registrar(OrigenAuditoria origen, AccionAuditoria accion, EntidadAuditoria entidad, Object entidadId,
                          Object valorAnterior, Object valorNuevo) {
        registrarComo(usuarioDeLaPeticion(), origen, accion, entidad, entidadId, valorAnterior, valorNuevo);
    }

    /** Igual que {@link #registrar}, indicando quién lo hizo (al iniciar sesión todavía no hay token). */
    public void registrarComo(Usuario usuario, OrigenAuditoria origen, AccionAuditoria accion, EntidadAuditoria entidad,
                              Object entidadId, Object valorAnterior, Object valorNuevo) {
        registros.save(new RegistroAuditoria(usuario, origen, accion, entidad,
                entidadId == null ? null : entidadId.toString(), aJson(valorAnterior), aJson(valorNuevo),
                ipDeLaPeticion(), LocalDateTime.now(clock)));
    }

    /**
     * Búsqueda con filtros opcionales, de lo más reciente a lo más antiguo.
     * Las fechas son días completos en hora de Madrid: desde el inicio de "desde" hasta el final de "hasta".
     */
    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> buscar(EntidadAuditoria entidad, String entidadId, Long usuarioId,
                                                    AccionAuditoria accion, LocalDate desde, LocalDate hasta,
                                                    int pagina, int tamano) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", "La fecha final no puede ser anterior a la inicial.");
        }
        PageRequest orden = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "creadoEn", "id"));
        String id = entidadId == null || entidadId.isBlank() ? null : entidadId.trim();
        return PaginaResponse.de(registros.buscar(entidad, id, usuarioId, accion,
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(), orden), this::aRespuesta);
    }

    private AuditoriaResponse aRespuesta(RegistroAuditoria r) {
        return new AuditoriaResponse(r.getId(), r.getCreadoEn(),
                r.getUsuario() == null ? null : UsuarioResumen.from(r.getUsuario()),
                r.getOrigen(), r.getAccion(), r.getEntidad(), r.getEntidadId(),
                leer(r.getValorAnterior()), leer(r.getValorNuevo()), r.getIp());
    }

    private Usuario usuarioDeLaPeticion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuarios.getReferenceById(usuario.id());
        }
        return null;
    }

    private static String ipDeLaPeticion() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String ip = atributos.getRequest().getRemoteAddr();
            return ip == null || ip.length() <= LONGITUD_IP ? ip : ip.substring(0, LONGITUD_IP);
        }
        return null;
    }

    private String aJson(Object valor) {
        if (valor == null) return null;
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar el detalle de la auditoría.", e);
        }
    }

    private JsonNode leer(String valor) {
        if (valor == null) return null;
        try {
            return json.readTree(valor);
        } catch (JsonProcessingException e) {
            return TextNode.valueOf(valor);
        }
    }
}
