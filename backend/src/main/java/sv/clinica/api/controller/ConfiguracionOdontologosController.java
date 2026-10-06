package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.ActivacionRequest;
import sv.clinica.api.dto.OdontologoConfigRequest;
import sv.clinica.api.dto.OdontologoConfigResponse;
import sv.clinica.api.dto.UsuarioParaOdontologoResponse;
import sv.clinica.api.service.ConfiguracionOdontologosService;

import java.util.List;

/**
 * Odontólogos desde el software (permiso odontologos.gestionar). La landing sigue leyendo los activos en la ruta
 * pública /api/odontologos, que no cambia.
 */
@Tag(name = "Configuración de la agenda", description = "Odontólogos, tratamientos, horarios y bloqueos")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/configuracion/odontologos")
@PreAuthorize("hasAuthority('odontologos.gestionar')")
public class ConfiguracionOdontologosController {

    private final ConfiguracionOdontologosService service;

    public ConfiguracionOdontologosController(ConfiguracionOdontologosService service) {
        this.service = service;
    }

    @Operation(summary = "Todos los odontólogos", description = "Permiso: odontologos.gestionar. También los desactivados.")
    @GetMapping
    public List<OdontologoConfigResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Dar de alta un odontólogo",
            description = "Permiso: odontologos.gestionar. Sale en la web al momento; para tener huecos libres necesita turnos.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OdontologoConfigResponse crear(@Valid @RequestBody OdontologoConfigRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Cambiar sus datos y su usuario del software",
            description = "Permiso: odontologos.gestionar. 409 si el usuario ya está vinculado a otro odontólogo.")
    @PutMapping("/{id}")
    public OdontologoConfigResponse editar(@PathVariable Long id, @Valid @RequestBody OdontologoConfigRequest request) {
        return service.editar(id, request);
    }

    @Operation(summary = "Activar o desactivar",
            description = "Permiso: odontologos.gestionar. No se borra nada. 409 si aún tiene citas pendientes o confirmadas.")
    @PutMapping("/{id}/estado")
    public OdontologoConfigResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody ActivacionRequest request) {
        return service.cambiarEstado(id, request.activo());
    }

    @Operation(summary = "Usuarios que se pueden vincular",
            description = "Permiso: odontologos.gestionar. Usuarios activos, con el odontólogo al que ya está unido cada uno.")
    @GetMapping("/usuarios")
    public List<UsuarioParaOdontologoResponse> usuarios() {
        return service.usuariosParaVincular();
    }
}
