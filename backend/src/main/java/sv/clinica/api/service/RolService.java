package sv.clinica.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.PermisoResponse;
import sv.clinica.api.dto.RolResponse;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.Permiso;
import sv.clinica.api.entity.Rol;
import sv.clinica.api.exception.ConflictoException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.PermisoRepository;
import sv.clinica.api.repository.RolRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Roles y sus permisos. Los roles y los permisos los crean las migraciones; desde el software se decide
 * qué permisos tiene cada rol. El administrador tiene siempre todos y no se puede cambiar.
 */
@Service
public class RolService {

    private final RolRepository roles;
    private final PermisoRepository permisos;
    private final AuditoriaService auditoria;

    public RolService(RolRepository roles, PermisoRepository permisos, AuditoriaService auditoria) {
        this.roles = roles;
        this.permisos = permisos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        Map<Long, Long> usuarios = usuariosPorRol();
        return roles.findAllByOrderByIdAsc().stream()
                .map(r -> RolResponse.from(r, usuarios.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoResponse> listarPermisos() {
        return permisos.findAllByOrderByIdAsc().stream().map(PermisoResponse::from).toList();
    }

    /** Sustituye los permisos del rol. El cambio se aplica al momento a todos los usuarios con ese rol. */
    @Transactional
    public RolResponse cambiarPermisos(Long id, List<String> codigos) {
        Rol rol = roles.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El rol no existe."));
        if (rol.esAdministrador()) {
            throw new ConflictoException("El administrador tiene siempre todos los permisos: no se pueden cambiar.");
        }
        Set<String> pedidos = codigos.stream().filter(Objects::nonNull).map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Permiso> encontrados = permisos.findByCodigoIn(pedidos);
        if (encontrados.size() != pedidos.size()) {
            throw new DatosInvalidosException("permisos", "Alguno de los permisos elegidos no existe.");
        }

        List<String> antes = codigos(rol);
        rol.cambiarPermisos(encontrados);
        List<String> despues = codigos(rol);
        if (!antes.equals(despues)) {
            auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CAMBIAR_PERMISOS, EntidadAuditoria.ROL, id,
                    Map.of("permisos", antes), Map.of("permisos", despues));
        }
        return RolResponse.from(rol, usuariosPorRol().getOrDefault(id, 0L));
    }

    private Map<Long, Long> usuariosPorRol() {
        return roles.contarUsuariosPorRol().stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]));
    }

    private static List<String> codigos(Rol rol) {
        return rol.getPermisos().stream().map(Permiso::getCodigo).sorted().toList();
    }
}
