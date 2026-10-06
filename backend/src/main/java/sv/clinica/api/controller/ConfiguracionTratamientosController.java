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
import sv.clinica.api.dto.TratamientoConfigRequest;
import sv.clinica.api.dto.TratamientoConfigResponse;
import sv.clinica.api.service.ConfiguracionTratamientosService;

import java.util.List;

/**
 * Tratamientos desde el software (permiso tratamientos.gestionar). La landing sigue leyendo los activos en la ruta
 * pública /api/tratamientos, que no cambia.
 */
@Tag(name = "Configuración de la agenda", description = "Odontólogos, tratamientos, horarios y bloqueos")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/configuracion/tratamientos")
@PreAuthorize("hasAuthority('tratamientos.gestionar')")
public class ConfiguracionTratamientosController {

    private final ConfiguracionTratamientosService service;

    public ConfiguracionTratamientosController(ConfiguracionTratamientosService service) {
        this.service = service;
    }

    @Operation(summary = "Todos los tratamientos", description = "Permiso: tratamientos.gestionar. También los desactivados.")
    @GetMapping
    public List<TratamientoConfigResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Dar de alta un tratamiento",
            description = "Permiso: tratamientos.gestionar. La duración va de 15 en 15 minutos.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TratamientoConfigResponse crear(@Valid @RequestBody TratamientoConfigRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Cambiar duración, precio, quién lo hace…",
            description = "Permiso: tratamientos.gestionar. Las citas ya dadas no cambian; las nuevas usan la duración nueva.")
    @PutMapping("/{id}")
    public TratamientoConfigResponse editar(@PathVariable Long id, @Valid @RequestBody TratamientoConfigRequest request) {
        return service.editar(id, request);
    }

    @Operation(summary = "Ofrecerlo o dejar de ofrecerlo",
            description = "Permiso: tratamientos.gestionar. No se borra nada. 409 si aún tiene citas pendientes o confirmadas.")
    @PutMapping("/{id}/estado")
    public TratamientoConfigResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody ActivacionRequest request) {
        return service.cambiarEstado(id, request.activo());
    }
}
