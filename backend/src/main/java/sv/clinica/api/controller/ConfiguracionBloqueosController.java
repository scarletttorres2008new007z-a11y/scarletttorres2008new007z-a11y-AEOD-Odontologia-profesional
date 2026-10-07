package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.BloqueoGuardadoResponse;
import sv.clinica.api.dto.BloqueoRequest;
import sv.clinica.api.dto.BloqueoResponse;
import sv.clinica.api.service.ConfiguracionBloqueosService;

import java.util.List;

/** Tiempo bloqueado en la agenda: festivos, vacaciones, reuniones… (permiso bloqueos.gestionar). */
@Tag(name = "Configuración de la agenda", description = "Odontólogos, tratamientos, horarios y bloqueos")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/configuracion/bloqueos")
@PreAuthorize("hasAuthority('bloqueos.gestionar')")
public class ConfiguracionBloqueosController {

    private final ConfiguracionBloqueosService service;

    public ConfiguracionBloqueosController(ConfiguracionBloqueosService service) {
        this.service = service;
    }

    @Operation(summary = "Bloqueos en vigor", description = "Permiso: bloqueos.gestionar. Los que no han terminado.")
    @GetMapping
    public List<BloqueoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Bloquear tiempo",
            description = "Permiso: bloqueos.gestionar. Se aplica al momento en la web y en el software. Devuelve las "
                    + "citas que ya había en ese tiempo (no se cancelan).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueoGuardadoResponse crear(@Valid @RequestBody BloqueoRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Cambiar un bloqueo", description = "Permiso: bloqueos.gestionar.")
    @PutMapping("/{id}")
    public BloqueoGuardadoResponse editar(@PathVariable Long id, @Valid @RequestBody BloqueoRequest request) {
        return service.editar(id, request);
    }

    @Operation(summary = "Quitar un bloqueo",
            description = "Permiso: bloqueos.gestionar. No se borra: queda desactivado y en la auditoría, y ese tiempo "
                    + "vuelve a estar libre al momento.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable Long id) {
        service.quitar(id);
    }
}
