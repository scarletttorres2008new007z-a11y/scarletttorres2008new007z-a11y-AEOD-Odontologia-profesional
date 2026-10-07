package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.PermisoResponse;
import sv.clinica.api.dto.PermisosRolRequest;
import sv.clinica.api.dto.RolResponse;
import sv.clinica.api.service.RolService;

import java.util.List;

@Tag(name = "Roles y permisos", description = "Qué puede hacer cada rol")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api")
public class RolController {

    private final RolService service;

    public RolController(RolService service) {
        this.service = service;
    }

    @Operation(summary = "Roles con sus permisos",
            description = "Permiso: roles.ver o usuarios.asignar_roles (para elegir los roles de un usuario).")
    @PreAuthorize("hasAnyAuthority('roles.ver', 'usuarios.asignar_roles')")
    @GetMapping("/roles")
    public List<RolResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Todos los permisos que existen", description = "Permiso: roles.ver.")
    @PreAuthorize("hasAuthority('roles.ver')")
    @GetMapping("/permisos")
    public List<PermisoResponse> permisos() {
        return service.listarPermisos();
    }

    @Operation(summary = "Cambiar los permisos de un rol",
            description = "Permiso: roles.editar. Sustituye todos sus permisos. Los del administrador no se pueden cambiar (409).")
    @PreAuthorize("hasAuthority('roles.editar')")
    @PutMapping("/roles/{id}/permisos")
    public RolResponse cambiarPermisos(@PathVariable Long id, @Valid @RequestBody PermisosRolRequest request) {
        return service.cambiarPermisos(id, request.permisos());
    }
}
