package sv.clinica.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import sv.clinica.api.config.SeguridadProperties;
import sv.clinica.api.dto.CambioPasswordRequest;
import sv.clinica.api.dto.LoginRequest;
import sv.clinica.api.dto.SesionResponse;
import sv.clinica.api.dto.UsuarioActualResponse;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.Sesion;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.exception.CredencialesIncorrectasException;
import sv.clinica.api.exception.CuentaBloqueadaException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.SesionNoValidaException;
import sv.clinica.api.repository.SesionRepository;
import sv.clinica.api.repository.UsuarioRepository;
import sv.clinica.api.security.TokenService;
import sv.clinica.api.security.UsuarioAutenticado;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inicio y cierre de sesión del software de gestión.
 *
 * Al entrar se crea una sesión en la base de datos y se entregan dos cosas: un token de acceso corto (JWT) para
 * las peticiones y un token de refresco largo que viaja solo en una cookie HttpOnly. De este último solo se guarda
 * su huella SHA-256. Cada petición comprueba que su sesión sigue abierta y relee los permisos del usuario.
 */
@Service
public class AuthService {

    private static final int BYTES_TOKEN_REFRESCO = 32;
    private static final int LONGITUD_IP = 45;
    private static final int LONGITUD_USER_AGENT = 255;
    private static final String DESACTIVADO = "Tu usuario está desactivado. Habla con el administrador de la clínica.";

    private final UsuarioRepository usuarios;
    private final SesionRepository sesiones;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final AuditoriaService auditoria;
    private final SeguridadProperties seguridad;
    private final TransactionTemplate transaccion;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();
    /** Hash de una contraseña al azar: con un usuario que no existe se comprueba contra él, para tardar lo mismo. */
    private final String hashDeRelleno;

    public AuthService(UsuarioRepository usuarios, SesionRepository sesiones, PasswordEncoder passwords,
                       TokenService tokens, AuditoriaService auditoria, SeguridadProperties seguridad,
                       TransactionTemplate transaccion, Clock clock) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.passwords = passwords;
        this.tokens = tokens;
        this.auditoria = auditoria;
        this.seguridad = seguridad;
        this.transaccion = transaccion;
        this.clock = clock;
        this.hashDeRelleno = passwords.encode(UUID.randomUUID().toString());
    }

    /** Sesión recién iniciada: la respuesta para el software y el token de refresco para la cookie. */
    public record SesionIniciada(SesionResponse respuesta, String tokenRefresco) {
    }

    private record Intento(SesionIniciada sesion, RuntimeException error) {
    }

    /**
     * Comprueba usuario (o correo) y contraseña. Tras {@code intentos-maximos} contraseñas incorrectas seguidas,
     * la cuenta queda bloqueada {@code minutos-bloqueo} minutos.
     *
     * @throws CredencialesIncorrectasException 401, sin decir si falló el usuario o la contraseña
     * @throws CuentaBloqueadaException         423
     */
    public SesionIniciada iniciarSesion(LoginRequest request, String ip, String userAgent) {
        String identificador = request.usuario().trim().toLowerCase(Locale.ROOT);
        // El contador de fallos, el bloqueo y la auditoría se guardan aunque la respuesta sea un error
        Intento intento = transaccion.execute(estado -> intentarEntrar(identificador, request.password(), ip, userAgent));
        if (intento.error() != null) throw intento.error();
        return intento.sesion();
    }

    private Intento intentarEntrar(String identificador, String password, String ip, String userAgent) {
        LocalDateTime ahora = ahora();
        Usuario usuario = usuarios.findByUsernameOrEmail(identificador).orElse(null);
        if (usuario == null) {
            passwordCorrecta(password, hashDeRelleno);
            auditoria.registrarComo(null, OrigenAuditoria.SOFTWARE, AccionAuditoria.INICIO_SESION_FALLIDO,
                    EntidadAuditoria.USUARIO, null, null, Map.of("motivo", "USUARIO_DESCONOCIDO"));
            return fallo(new CredencialesIncorrectasException(CredencialesIncorrectasException.MENSAJE));
        }
        if (usuario.estaBloqueado(ahora)) {
            return fallo(new CuentaBloqueadaException(minutosHasta(usuario.getBloqueadoHasta(), ahora)));
        }
        if (!passwordCorrecta(password, usuario.getPasswordHash())) {
            boolean bloqueada = usuario.registrarIntentoFallido(seguridad.intentosMaximos(),
                    Duration.ofMinutes(seguridad.minutosBloqueo()), ahora);
            auditoria.registrarComo(null, OrigenAuditoria.SOFTWARE, AccionAuditoria.INICIO_SESION_FALLIDO,
                    EntidadAuditoria.USUARIO, usuario.getId(), null, Map.of("motivo", "PASSWORD_INCORRECTA"));
            if (bloqueada) {
                auditoria.registrarComo(null, OrigenAuditoria.SOFTWARE, AccionAuditoria.BLOQUEAR_POR_INTENTOS,
                        EntidadAuditoria.USUARIO, usuario.getId(), null,
                        Map.of("bloqueado_hasta", usuario.getBloqueadoHasta().toString()));
                return fallo(new CuentaBloqueadaException(seguridad.minutosBloqueo()));
            }
            return fallo(new CredencialesIncorrectasException(CredencialesIncorrectasException.MENSAJE));
        }
        // Solo quien sabe la contraseña llega a saber que el usuario está desactivado
        if (!usuario.isActivo()) {
            return fallo(new CredencialesIncorrectasException(DESACTIVADO));
        }
        usuario.registrarAcceso(ahora);
        String tokenRefresco = nuevoTokenDeRefresco();
        Sesion sesion = sesiones.save(new Sesion(usuario, huella(tokenRefresco), ahora,
                ahora.plusHours(seguridad.horasSesion()), recortar(ip, LONGITUD_IP), recortar(userAgent, LONGITUD_USER_AGENT)));
        auditoria.registrarComo(usuario, OrigenAuditoria.SOFTWARE, AccionAuditoria.INICIAR_SESION,
                EntidadAuditoria.USUARIO, usuario.getId(), null, null);
        return new Intento(new SesionIniciada(respuesta(usuario, sesion), tokenRefresco), null);
    }

    /** Nuevo token de acceso para una sesión abierta (el software lo pide al caducar el anterior o al recargar). */
    @Transactional(readOnly = true)
    public SesionResponse renovar(String tokenRefresco) {
        Sesion sesion = sesionActiva(tokenRefresco).orElseThrow(SesionNoValidaException::new);
        return respuesta(sesion.getUsuario(), sesion);
    }

    /** Cierra la sesión: su token de refresco y sus tokens de acceso dejan de valer al momento. */
    @Transactional
    public void cerrarSesion(String tokenRefresco) {
        sesionActiva(tokenRefresco).ifPresent(sesion -> {
            sesion.revocar(ahora());
            auditoria.registrarComo(sesion.getUsuario(), OrigenAuditoria.SOFTWARE, AccionAuditoria.CERRAR_SESION,
                    EntidadAuditoria.USUARIO, sesion.getUsuario().getId(), null, null);
        });
    }

    /**
     * Lo comprueba cada petición con token: la sesión sigue abierta, es de ese usuario y el usuario está activo.
     * Devuelve sus permisos actuales, así quitar un permiso o desactivar a alguien tiene efecto inmediato.
     */
    @Transactional(readOnly = true)
    public Optional<UsuarioAutenticado> validarSesion(Long sesionId, Long usuarioId) {
        LocalDateTime ahora = ahora();
        return sesiones.findConUsuario(sesionId)
                .filter(s -> s.getUsuario().getId().equals(usuarioId) && s.estaActiva(ahora) && s.getUsuario().isActivo())
                .map(s -> new UsuarioAutenticado(usuarioId, s.getUsuario().getUsername(), s.getUsuario().getNombre(),
                        s.getId(), Set.copyOf(usuarios.findCodigosDePermisos(usuarioId))));
    }

    @Transactional(readOnly = true)
    public UsuarioActualResponse usuarioActual(UsuarioAutenticado actual) {
        Usuario usuario = usuarios.findById(actual.id()).orElseThrow(SesionNoValidaException::new);
        return UsuarioActualResponse.from(usuario, actual.permisos());
    }

    /** Cambio de la propia contraseña. Cierra las demás sesiones del usuario (otros equipos), no la actual. */
    @Transactional
    public void cambiarPassword(UsuarioAutenticado actual, CambioPasswordRequest request) {
        Usuario usuario = usuarios.findById(actual.id()).orElseThrow(SesionNoValidaException::new);
        if (!passwordCorrecta(request.passwordActual(), usuario.getPasswordHash())) {
            throw new DatosInvalidosException("password_actual", "La contraseña actual no es correcta.");
        }
        PoliticaDePasswords.comprobar("password_nueva", request.passwordNueva(), usuario.getUsername(), usuario.getEmail());
        if (passwords.matches(request.passwordNueva(), usuario.getPasswordHash())) {
            throw new DatosInvalidosException("password_nueva", "La contraseña nueva debe ser distinta de la actual.");
        }
        LocalDateTime ahora = ahora();
        usuario.cambiarPassword(passwords.encode(request.passwordNueva()), ahora);
        sesiones.revocarDelUsuario(usuario.getId(), actual.sesionId(), ahora);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CAMBIAR_PASSWORD,
                EntidadAuditoria.USUARIO, usuario.getId(), null, null);
    }

    private Optional<Sesion> sesionActiva(String tokenRefresco) {
        if (tokenRefresco == null || tokenRefresco.isBlank()) return Optional.empty();
        LocalDateTime ahora = ahora();
        return sesiones.findByRefreshTokenHash(huella(tokenRefresco))
                .filter(s -> s.estaActiva(ahora) && s.getUsuario().isActivo());
    }

    private SesionResponse respuesta(Usuario usuario, Sesion sesion) {
        return new SesionResponse(tokens.emitir(usuario.getId(), sesion.getId()), "Bearer", tokens.segundosDeValidez(),
                UsuarioActualResponse.from(usuario, usuarios.findCodigosDePermisos(usuario.getId())));
    }

    /**
     * BCrypt solo mira los primeros 72 bytes: una contraseña más larga nunca puede ser la buena
     * (no se permite guardarla), así que se rechaza sin compararla, pero tardando lo mismo.
     */
    private boolean passwordCorrecta(String password, String hash) {
        if (password == null || PoliticaDePasswords.demasiadoLarga(password)) {
            passwords.matches("", hashDeRelleno);
            return false;
        }
        return passwords.matches(password, hash);
    }

    private String nuevoTokenDeRefresco() {
        byte[] bytes = new byte[BYTES_TOKEN_REFRESCO];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Huella SHA-256 (hex) del token de refresco: es lo único que se guarda de él. */
    static String huella(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    private static long minutosHasta(LocalDateTime hasta, LocalDateTime ahora) {
        long segundos = Duration.between(ahora, hasta).toSeconds();
        return Math.max(1, (segundos + 59) / 60);
    }

    private static String recortar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private static Intento fallo(RuntimeException error) {
        return new Intento(null, error);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
