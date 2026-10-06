package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.EstadoPacienteRequest;
import sv.clinica.api.dto.PacienteRequest;
import sv.clinica.api.dto.PacienteResponse;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.service.PacienteService;

/** Pacientes. Cada operación exige su permiso; sin él, la API responde 403 aunque se llame directamente. */
@Tag(name = "Pacientes", description = "Datos personales, de contacto y administrativos de los pacientes")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @Operation(summary = "Buscar pacientes", description = "Permiso: pacientes.ver. Ordenados por apellidos y nombre.")
    @PreAuthorize("hasAuthority('pacientes.ver')")
    @GetMapping
    public PaginaResponse<PacienteResponse> buscar(
            @Parameter(description = "Palabras del nombre, los apellidos, el documento, el teléfono, el correo o el código")
            @RequestParam(required = false) String texto,
            @Parameter(description = "true = activos, false = dados de baja, vacío = todos") @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return service.buscar(texto, activo, Paginas.pagina(pagina), Paginas.tamano(tamano));
    }

    @Operation(summary = "Ver un paciente", description = "Permiso: pacientes.ver.")
    @PreAuthorize("hasAuthority('pacientes.ver')")
    @GetMapping("/{id}")
    public PacienteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Dar de alta un paciente", description = "Permiso: pacientes.crear. El código del paciente lo genera el backend.")
    @PreAuthorize("hasAuthority('pacientes.crear')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse crear(@Valid @RequestBody PacienteRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Editar sus datos", description = "Permiso: pacientes.editar. Sustituye todos sus datos.")
    @PreAuthorize("hasAuthority('pacientes.editar')")
    @PutMapping("/{id}")
    public PacienteResponse editar(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return service.editar(id, request);
    }

    @Operation(summary = "Dar de baja o reactivar", description = "Permiso: pacientes.editar. Dar de baja no borra nada.")
    @PreAuthorize("hasAuthority('pacientes.editar')")
    @PutMapping("/{id}/estado")
    public PacienteResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPacienteRequest request) {
        return service.cambiarEstado(id, request.activo());
    }
}
