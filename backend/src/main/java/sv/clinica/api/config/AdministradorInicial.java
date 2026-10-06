package sv.clinica.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.Rol;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.repository.RolRepository;
import sv.clinica.api.repository.SesionRepository;
import sv.clinica.api.repository.UsuarioRepository;
import sv.clinica.api.service.AuditoriaService;
import sv.clinica.api.service.PoliticaDePasswords;
import sv.clinica.api.service.UsuarioService;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Primer administrador del software, sin contraseñas en el código. Al arrancar:
 * - con ADMIN_PASSWORD: el usuario ADMIN_USERNAME (por defecto "admin") queda activo, administrador y con esa
 *   contraseña; si no existe se crea con ADMIN_EMAIL. Sirve también para recuperar el acceso. Después, quita la variable.
 * - sin ADMIN_PASSWORD y sin ningún administrador activo: en tu equipo (dev) se crea con una contraseña aleatoria
 *   que se muestra una sola vez en la consola; en test y prod solo avisa de que falta.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);
    /** Sin caracteres que se confunden (0/O, 1/l/I). */
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_GENERADA = 16;

    private final AdministradorInicialProperties propiedades;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final SesionRepository sesiones;
    private final PasswordEncoder passwords;
    private final AuditoriaService auditoria;
    private final TransactionTemplate transaccion;
    private final Clock clock;

    public AdministradorInicial(AdministradorInicialProperties propiedades, UsuarioRepository usuarios,
                                RolRepository roles, SesionRepository sesiones, PasswordEncoder passwords,
                                AuditoriaService auditoria, TransactionTemplate transaccion, Clock clock) {
        this.propiedades = propiedades;
        this.usuarios = usuarios;
        this.roles = roles;
        this.sesiones = sesiones;
        this.passwords = passwords;
        this.auditoria = auditoria;
        this.transaccion = transaccion;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        transaccion.executeWithoutResult(estado -> preparar());
    }

    private void preparar() {
        String username = propiedades.username().trim().toLowerCase(Locale.ROOT);
        String password = propiedades.password();
        if (password != null && !password.isBlank()) {
            aplicar(username, password, "ADMIN_PASSWORD");
            log.warn("Administrador «{}» preparado con ADMIN_PASSWORD. Por seguridad, quita ya la variable "
                    + "ADMIN_PASSWORD: la contraseña sigue guardada (cifrada) en la base de datos.", username);
            return;
        }
        if (usuarios.contarAdministradoresActivos() > 0) return;
        if (!propiedades.generarPassword()) {
            log.warn("No hay ningún administrador activo. Para crear el primero, define ADMIN_USERNAME, ADMIN_EMAIL "
                    + "y ADMIN_PASSWORD y reinicia el backend (ver backend/README.md).");
            return;
        }
        String generada = generarPassword();
        aplicar(username, generada, "la contraseña generada");
        log.warn("""

                ════════════════════════════════════════════════════════════
                  Administrador del software de gestión (solo en tu equipo)
                    Usuario:    {}
                    Contraseña: {}
                  Apúntala ahora: no se vuelve a mostrar. Al entrar,
                  cámbiala en «Mi cuenta».
                ════════════════════════════════════════════════════════════""", username, generada);
    }

    /** Deja al usuario activo, desbloqueado, administrador y con esa contraseña (lo crea si no existe). */
    private void aplicar(String username, String password, String origen) {
        Rol administrador = roles.findByCodigo(Rol.ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("Falta el rol ADMINISTRADOR (migración V2)."));
        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario usuario = usuarios.findByUsername(username).orElse(null);
        if (usuario == null) {
            String email = email();
            comprobarPolitica(password, username, email, origen);
            if (usuarios.existsByEmail(email)) {
                throw new IllegalStateException("El correo de ADMIN_EMAIL ya lo usa otro usuario.");
            }
            usuario = new Usuario(username, email, "Administrador", passwords.encode(password), ahora);
            usuario.cambiarRoles(Set.of(administrador), ahora);
            usuarios.save(usuario);
            auditoria.registrarComo(null, OrigenAuditoria.SISTEMA, AccionAuditoria.CREAR, EntidadAuditoria.USUARIO,
                    usuario.getId(), null, UsuarioService.datosAuditables(usuario));
            return;
        }

        comprobarPolitica(password, usuario.getUsername(), usuario.getEmail(), origen);
        if (!usuario.isActivo() || usuario.estaBloqueado(ahora)) {
            usuario.cambiarEstado(true, ahora);
            auditoria.registrarComo(null, OrigenAuditoria.SISTEMA, AccionAuditoria.ACTIVAR, EntidadAuditoria.USUARIO,
                    usuario.getId(), null, Map.of("activo", true, "bloqueado", false));
        }
        if (!usuario.tieneRol(Rol.ADMINISTRADOR)) {
            Set<Rol> nuevos = new LinkedHashSet<>(usuario.getRoles());
            nuevos.add(administrador);
            usuario.cambiarRoles(nuevos, ahora);
            auditoria.registrarComo(null, OrigenAuditoria.SISTEMA, AccionAuditoria.CAMBIAR_ROLES, EntidadAuditoria.USUARIO,
                    usuario.getId(), null, Map.of("roles", usuario.getRoles().stream().map(Rol::getCodigo).sorted().toList()));
        }
        if (!passwords.matches(password, usuario.getPasswordHash())) {
            usuario.cambiarPassword(passwords.encode(password), ahora);
            sesiones.revocarDelUsuario(usuario.getId(), null, ahora);
            auditoria.registrarComo(null, OrigenAuditoria.SISTEMA, AccionAuditoria.RESTABLECER_PASSWORD,
                    EntidadAuditoria.USUARIO, usuario.getId(), null, null);
        }
    }

    private String email() {
        String email = propiedades.email();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Falta ADMIN_EMAIL: el correo del primer administrador.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void comprobarPolitica(String password, String username, String email, String origen) {
        try {
            PoliticaDePasswords.comprobar("password", password, username, email);
        } catch (DatosInvalidosException e) {
            throw new IllegalStateException("No se puede usar " + origen + ": " + e.getMessage());
        }
    }

    private static String generarPassword() {
        SecureRandom aleatorio = new SecureRandom();
        StringBuilder password = new StringBuilder(LONGITUD_GENERADA);
        for (int i = 0; i < LONGITUD_GENERADA; i++) {
            password.append(CARACTERES.charAt(aleatorio.nextInt(CARACTERES.length())));
        }
        return password.toString();
    }
}
