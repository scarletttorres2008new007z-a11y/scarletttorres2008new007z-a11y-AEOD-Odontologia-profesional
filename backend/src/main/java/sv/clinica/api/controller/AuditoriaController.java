package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.AuditoriaResponse;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.service.AuditoriaService;

import java.time.LocalDate;

@Tag(name = "Auditoría", description = "Registro de las operaciones importantes")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @Operation(summary = "Consultar la auditoría", description = "Permiso: auditoria.ver. De lo más reciente a lo más antiguo.")
    @PreAuthorize("hasAuthority('auditoria.ver')")
    @GetMapping
    public PaginaResponse<AuditoriaResponse> buscar(
            @RequestParam(required = false) EntidadAuditoria entidad,
            @Parameter(description = "Identificador del registro afectado") @RequestParam(name = "entidad_id", required = false) String entidadId,
            @Parameter(description = "Quién lo hizo") @RequestParam(name = "usuario_id", required = false) Long usuarioId,
            @RequestParam(required = false) AccionAuditoria accion,
            @Parameter(description = "Desde este día (incluido), hora de Madrid") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Hasta este día (incluido), hora de Madrid") @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return service.buscar(entidad, entidadId, usuarioId, accion, desde, hasta,
                Paginas.pagina(pagina), Paginas.tamano(tamano));
    }
}
