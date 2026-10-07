package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.CambioPasswordRequest;
import sv.clinica.api.dto.LoginRequest;
import sv.clinica.api.dto.SesionResponse;
import sv.clinica.api.dto.UsuarioActualResponse;
import sv.clinica.api.exception.SesionNoValidaException;
import sv.clinica.api.security.CookieDeSesion;
import sv.clinica.api.security.UsuarioAutenticado;
import sv.clinica.api.service.AuthService;

@Tag(name = "Sesión", description = "Entrar y salir del software de gestión")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Cabecera que solo puede poner el propio software: otra web no puede renovar ni cerrar la sesión. */
    static final String CABECERA_SOFTWARE = "X-Requested-With";
    private static final String CABECERA_DESCRIPCION =
            "Siempre XMLHttpRequest. Protege la cookie de sesión frente a peticiones desde otras webs.";

    private final AuthService auth;
    private final CookieDeSesion cookie;

    public AuthController(AuthService auth, CookieDeSesion cookie) {
        this.auth = auth;
        this.cookie = cookie;
    }

    @Operation(summary = "Iniciar sesión",
            description = "Con usuario o correo y contraseña. Devuelve el token de acceso y deja el de refresco en una "
                    + "cookie HttpOnly. 401 si los datos no son correctos; 423 si la cuenta está bloqueada por intentos fallidos.")
    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthService.SesionIniciada sesion = auth.iniciarSesion(request, http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.crear(sesion.tokenRefresco()))
                .body(sesion.respuesta());
    }

    @Operation(summary = "Renovar el token de acceso",
            description = "Usa la cookie de sesión. 401 si la sesión caducó o se cerró.")
    @PostMapping("/refresh")
    public SesionResponse renovar(@Parameter(hidden = true) @CookieValue(name = CookieDeSesion.NOMBRE, required = false) String token,
                                  @Parameter(required = true, example = "XMLHttpRequest", description = CABECERA_DESCRIPCION)
                                  @RequestHeader(name = CABECERA_SOFTWARE, required = false) String origen,
                                  HttpServletResponse response) {
        exigirCabecera(origen);
        try {
            return auth.renovar(token);
        } catch (SesionNoValidaException e) {
            // La cookie ya no sirve: se borra del navegador
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.borrar());
            throw e;
        }
    }

    @Operation(summary = "Cerrar sesión", description = "Cierra la sesión de la cookie y la borra.")
    @PostMapping("/logout")
    public ResponseEntity<Void> cerrar(@Parameter(hidden = true) @CookieValue(name = CookieDeSesion.NOMBRE, required = false) String token,
                                       @Parameter(required = true, example = "XMLHttpRequest", description = CABECERA_DESCRIPCION)
                                       @RequestHeader(name = CABECERA_SOFTWARE, required = false) String origen) {
        exigirCabecera(origen);
        auth.cerrarSesion(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar()).build();
    }

    @Operation(summary = "Usuario con sesión iniciada", description = "Sus datos, roles y permisos.")
    @SecurityRequirement(name = "bearer")
    @GetMapping("/me")
    public UsuarioActualResponse yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return auth.usuarioActual(usuario);
    }

    @Operation(summary = "Cambiar mi contraseña",
            description = "Pide la contraseña actual. Cierra las sesiones abiertas en otros equipos.")
    @SecurityRequirement(name = "bearer")
    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @Valid @RequestBody CambioPasswordRequest request) {
        auth.cambiarPassword(usuario, request);
        return ResponseEntity.noContent().build();
    }

    private static void exigirCabecera(String valor) {
        if (!"XMLHttpRequest".equals(valor)) {
            throw new AccessDeniedException("Falta la cabecera " + CABECERA_SOFTWARE);
        }
    }
}
