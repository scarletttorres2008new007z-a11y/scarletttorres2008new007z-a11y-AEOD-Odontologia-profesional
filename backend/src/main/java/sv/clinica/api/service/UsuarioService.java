package sv.clinica.api.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.dto.UsuarioCreacionRequest;
import sv.clinica.api.dto.UsuarioEdicionRequest;
import sv.clinica.api.dto.UsuarioResponse;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.Rol;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.exception.ConflictoException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.RolRepository;
import sv.clinica.api.repository.SesionRepository;
import sv.clinica.api.repository.UsuarioRepository;
import sv.clinica.api.security.Permisos;
import sv.clinica.api.security.UsuarioAutenticado;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Gestión de los usuarios del software. Los permisos de cada operación los comprueba el controller; aquí se
 * aplican las reglas que dependen de los datos:
 * - solo un administrador puede cambiar a otro administrador o dar el rol de administrador;
 * - nadie puede desactivarse a sí mismo ni quitarse su propio rol de administrador;
 * - la clínica nunca se queda sin ningún administrador activo.
 */
@Service
public class UsuarioService {

    private static final String SOLO_ADMINISTRADOR = "Solo un administrador puede cambiar a un administrador.";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final SesionRepository sesiones;
    private final PasswordEncoder passwords;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, SesionRepository sesiones,
                          PasswordEncoder passwords, AuditoriaService auditoria, Clock clock) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.sesiones = sesiones;
        this.passwords = passwords;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** Busca por nombre, usuario o correo; {@code activo} vacío = todos. Ordenados por nombre. */
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> buscar(String texto, Boolean activo, int pagina, int tamano) {
        LocalDateTime ahora = ahora();
        return PaginaResponse.de(usuarios.buscar(patronDeBusqueda(texto), activo,
                        PageRequest.of(pagina, tamano, Sort.by("nombre", "id"))),
                u -> UsuarioResponse.from(u, ahora));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscarUsuario(id), ahora());
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCreacionRequest request, UsuarioAutenticado actor) {
        if (request.roles() != null && !request.roles().isEmpty() && !actor.tienePermiso(Permisos.USUARIOS_ASIGNAR_ROLES)) {
            throw new AccessDeniedException("Asignar roles exige el permiso usuarios.asignar_roles.");
        }
        String username = normalizar(request.username());
        String email = normalizar(request.email());
        Set<Rol> nuevos = rolesExistentes(request.roles());
        if (nuevos.stream().anyMatch(Rol::esAdministrador) && !esAdministrador(actor)) {
            throw new AccessDeniedException(SOLO_ADMINISTRADOR);
        }
        comprobarQueEsUnico(username, email, null);
        PoliticaDePasswords.comprobar("password", request.password(), username, email);

        LocalDateTime ahora = ahora();
        Usuario usuario = new Usuario(username, email, request.nombre().trim(), passwords.encode(request.password()), ahora);
        usuario.cambiarRoles(nuevos, ahora);
        guardar(usuario);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CREAR, EntidadAuditoria.USUARIO,
                usuario.getId(), null, datosAuditables(usuario));
        return UsuarioResponse.from(usuario, ahora);
    }

    @Transactional
    public UsuarioResponse editar(Long id, UsuarioEdicionRequest request, UsuarioAutenticado actor) {
        Usuario usuario = buscarUsuario(id);
        comprobarQuePuedeCambiarlo(actor, usuario);
        String username = normalizar(request.username());
        String email = normalizar(request.email());
        comprobarQueEsUnico(username, email, id);

        String nombre = request.nombre().trim();
        LocalDateTime ahora = ahora();
        if (username.equals(usuario.getUsername()) && email.equals(usuario.getEmail()) && nombre.equals(usuario.getNombre())) {
            return UsuarioResponse.from(usuario, ahora);
        }
        Map<String, Object> antes = datosAuditables(usuario);
        usuario.editar(username, email, nombre, ahora);
        guardar(usuario);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.EDITAR, EntidadAuditoria.USUARIO, id,
                antes, datosAuditables(usuario));
        return UsuarioResponse.from(usuario, ahora);
    }

    /**
     * Activa o desactiva. Desactivar cierra todas sus sesiones al momento; activar también quita un bloqueo
     * por intentos fallidos.
     */
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, UsuarioAutenticado actor) {
        if (!activo && id.equals(actor.id())) {
            throw new ConflictoException("No puedes desactivar tu propio usuario.");
        }
        roles.bloquear(Rol.ADMINISTRADOR);
        Usuario usuario = buscarUsuario(id);
        comprobarQuePuedeCambiarlo(actor, usuario);
        LocalDateTime ahora = ahora();
        Map<String, Object> antes = estado(usuario, ahora);

        usuario.cambiarEstado(activo, ahora);
        Map<String, Object> despues = estado(usuario, ahora);
        if (antes.equals(despues)) return UsuarioResponse.from(usuario, ahora);
        if (!activo) {
            comprobarQueQuedaAdministrador();
            sesiones.revocarDelUsuario(id, null, ahora);
        }
        auditoria.registrar(OrigenAuditoria.SOFTWARE, activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR,
                EntidadAuditoria.USUARIO, id, antes, despues);
        return UsuarioResponse.from(usuario, ahora);
    }

    /** Cambia todos sus roles (los que no estén en la lista se quitan). */
    @Transactional
    public UsuarioResponse cambiarRoles(Long id, List<String> codigos, UsuarioAutenticado actor) {
        roles.bloquear(Rol.ADMINISTRADOR);
        Usuario usuario = buscarUsuario(id);
        Set<Rol> nuevos = rolesExistentes(codigos);
        boolean eraAdministrador = usuario.tieneRol(Rol.ADMINISTRADOR);
        boolean seraAdministrador = nuevos.stream().anyMatch(Rol::esAdministrador);
        if ((eraAdministrador || seraAdministrador) && !esAdministrador(actor)) {
            throw new AccessDeniedException(SOLO_ADMINISTRADOR);
        }
        if (eraAdministrador && !seraAdministrador && id.equals(actor.id())) {
            throw new ConflictoException("roles", "No puedes quitarte a ti mismo el rol de administrador.");
        }

        List<String> antes = codigos(usuario.getRoles());
        LocalDateTime ahora = ahora();
        usuario.cambiarRoles(nuevos, ahora);
        List<String> despues = codigos(usuario.getRoles());
        if (eraAdministrador && !seraAdministrador) comprobarQueQuedaAdministrador();
        if (!antes.equals(despues)) {
            auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CAMBIAR_ROLES, EntidadAuditoria.USUARIO, id,
                    Map.of("roles", antes), Map.of("roles", despues));
        }
        return UsuarioResponse.from(usuario, ahora);
    }

    /**
     * Pone una contraseña nueva a otro usuario (por ejemplo, si la olvidó). Le quita el bloqueo por intentos
     * y cierra todas sus sesiones. La propia se cambia desde «Mi cuenta», que pide la contraseña actual.
     */
    @Transactional
    public void restablecerPassword(Long id, String password, UsuarioAutenticado actor) {
        if (id.equals(actor.id())) {
            throw new ConflictoException("Para cambiar tu propia contraseña usa «Mi cuenta».");
        }
        Usuario usuario = buscarUsuario(id);
        comprobarQuePuedeCambiarlo(actor, usuario);
        PoliticaDePasswords.comprobar("password", password, usuario.getUsername(), usuario.getEmail());
        LocalDateTime ahora = ahora();
        usuario.cambiarPassword(passwords.encode(password), ahora);
        usuario.desbloquear();
        sesiones.revocarDelUsuario(id, null, ahora);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.RESTABLECER_PASSWORD, EntidadAuditoria.USUARIO,
                id, null, null);
    }

    /** Datos del usuario que se guardan en la auditoría (nunca la contraseña ni su hash). */
    public static Map<String, Object> datosAuditables(Usuario u) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("username", u.getUsername());
        datos.put("email", u.getEmail());
        datos.put("nombre", u.getNombre());
        datos.put("activo", u.isActivo());
        datos.put("roles", codigos(u.getRoles()));
        return datos;
    }

    private static Map<String, Object> estado(Usuario u, LocalDateTime ahora) {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("activo", u.isActivo());
        estado.put("bloqueado", u.estaBloqueado(ahora));
        return estado;
    }

    private static List<String> codigos(Collection<Rol> roles) {
        return roles.stream().map(Rol::getCodigo).sorted().toList();
    }

    private Usuario buscarUsuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }

    /** Un administrador solo lo puede cambiar otro administrador: si no, quien gestiona usuarios podría tomar su cuenta. */
    private void comprobarQuePuedeCambiarlo(UsuarioAutenticado actor, Usuario usuario) {
        if (usuario.tieneRol(Rol.ADMINISTRADOR) && !esAdministrador(actor)) {
            throw new AccessDeniedException(SOLO_ADMINISTRADOR);
        }
    }

    private boolean esAdministrador(UsuarioAutenticado actor) {
        return usuarios.findById(actor.id()).map(u -> u.tieneRol(Rol.ADMINISTRADOR)).orElse(false);
    }

    private void comprobarQueQuedaAdministrador() {
        if (usuarios.contarAdministradoresActivos() == 0) {
            throw new ConflictoException("La clínica tiene que tener al menos un administrador activo.");
        }
    }

    private void comprobarQueEsUnico(String username, String email, Long id) {
        boolean usernameOcupado = id == null ? usuarios.existsByUsername(username) : usuarios.existsByUsernameAndIdNot(username, id);
        if (usernameOcupado) {
            throw new ConflictoException("username", "Ya existe un usuario con ese nombre de usuario.");
        }
        boolean emailOcupado = id == null ? usuarios.existsByEmail(email) : usuarios.existsByEmailAndIdNot(email, id);
        if (emailOcupado) {
            throw new ConflictoException("email", "Ya existe un usuario con ese correo.");
        }
    }

    /** Si dos personas guardan a la vez el mismo usuario o correo, la base de datos rechaza el segundo. */
    private void guardar(Usuario usuario) {
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException repetido) {
            throw new ConflictoException("Ya existe un usuario con ese nombre de usuario o correo.");
        }
    }

    private Set<Rol> rolesExistentes(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) return Set.of();
        Set<String> pedidos = new LinkedHashSet<>();
        codigos.stream().filter(Objects::nonNull).map(c -> c.trim().toUpperCase(Locale.ROOT)).forEach(pedidos::add);
        List<Rol> encontrados = roles.findByCodigoIn(pedidos);
        if (encontrados.size() != pedidos.size()) {
            throw new DatosInvalidosException("roles", "Alguno de los roles elegidos no existe.");
        }
        return new LinkedHashSet<>(encontrados);
    }

    /** Usuario y correo se guardan siempre en minúsculas y sin espacios alrededor. */
    private static String normalizar(String valor) {
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private static String patronDeBusqueda(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String escapado = texto.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escapado + "%";
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
