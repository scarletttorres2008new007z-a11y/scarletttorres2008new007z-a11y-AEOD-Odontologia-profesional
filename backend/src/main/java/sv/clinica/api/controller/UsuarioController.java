package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.EstadoUsuarioRequest;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.dto.PasswordRequest;
import sv.clinica.api.dto.RolesUsuarioRequest;
import sv.clinica.api.dto.UsuarioCreacionRequest;
import sv.clinica.api.dto.UsuarioEdicionRequest;
import sv.clinica.api.dto.UsuarioResponse;
import sv.clinica.api.security.UsuarioAutenticado;
import sv.clinica.api.service.UsuarioService;

/** Usuarios del software. Cada operación exige su permiso; sin él, la API responde 403 aunque se llame directamente. */
@Tag(name = "Usuarios", description = "Personal de la clínica con acceso al software")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Buscar usuarios", description = "Permiso: usuarios.ver. Ordenados por nombre.")
    @PreAuthorize("hasAuthority('usuarios.ver')")
    @GetMapping
    public PaginaResponse<UsuarioResponse> buscar(
            @Parameter(description = "Parte del nombre, del usuario o del correo") @RequestParam(required = false) String texto,
            @Parameter(description = "true = activos, false = desactivados, vacío = todos") @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return service.buscar(texto, activo, Paginas.pagina(pagina), Paginas.tamano(tamano));
    }

    @Operation(summary = "Ver un usuario", description = "Permiso: usuarios.ver.")
    @PreAuthorize("hasAuthority('usuarios.ver')")
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear un usuario",
            description = "Permiso: usuarios.crear. Darle roles exige además usuarios.asignar_roles; el rol de administrador solo lo da un administrador.")
    @PreAuthorize("hasAuthority('usuarios.crear')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioCreacionRequest request,
                                 @AuthenticationPrincipal UsuarioAutenticado actor) {
        return service.crear(request, actor);
    }

    @Operation(summary = "Editar usuario, correo y nombre", description = "Permiso: usuarios.editar.")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    @PutMapping("/{id}")
    public UsuarioResponse editar(@PathVariable Long id, @Valid @RequestBody UsuarioEdicionRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado actor) {
        return service.editar(id, request, actor);
    }

    @Operation(summary = "Activar o desactivar",
            description = "Permiso: usuarios.editar. Desactivar cierra sus sesiones al momento; activar quita también un bloqueo por intentos fallidos.")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    @PutMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoUsuarioRequest request,
                                         @AuthenticationPrincipal UsuarioAutenticado actor) {
        return service.cambiarEstado(id, request.activo(), actor);
    }

    @Operation(summary = "Cambiar sus roles", description = "Permiso: usuarios.asignar_roles. Sustituye todos sus roles.")
    @PreAuthorize("hasAuthority('usuarios.asignar_roles')")
    @PutMapping("/{id}/roles")
    public UsuarioResponse cambiarRoles(@PathVariable Long id, @Valid @RequestBody RolesUsuarioRequest request,
                                        @AuthenticationPrincipal UsuarioAutenticado actor) {
        return service.cambiarRoles(id, request.roles(), actor);
    }

    @Operation(summary = "Restablecer su contraseña",
            description = "Permiso: usuarios.editar. Cierra todas sus sesiones. La propia contraseña se cambia en /api/auth/password.")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> restablecerPassword(@PathVariable Long id, @Valid @RequestBody PasswordRequest request,
                                                    @AuthenticationPrincipal UsuarioAutenticado actor) {
        service.restablecerPassword(id, request.password(), actor);
        return ResponseEntity.noContent().build();
    }
}
