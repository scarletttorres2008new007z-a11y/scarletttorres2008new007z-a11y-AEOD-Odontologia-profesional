package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.AgendaResponse;
import sv.clinica.api.dto.HuecoResponse;
import sv.clinica.api.service.AgendaService;

import java.time.LocalDate;
import java.util.List;

/** Agenda del software. Solo devuelve datos: qué hueco está libre lo calcula siempre el backend. */
@Tag(name = "Agenda", description = "Agenda por días y huecos libres para dar o mover citas")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    private final AgendaService service;

    public AgendaController(AgendaService service) {
        this.service = service;
    }

    @Operation(summary = "Agenda de uno o varios días",
            description = "Permiso: citas.ver. Sin citas.ver_todas, solo la del odontólogo vinculado al usuario.")
    @PreAuthorize("hasAuthority('citas.ver')")
    @GetMapping
    public AgendaResponse agenda(
            @Parameter(description = "Primer día") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Cuántos días, de 1 a 7") @RequestParam(defaultValue = "1") int dias,
            @Parameter(description = "Solo este odontólogo") @RequestParam(name = "odontologo_id", required = false) Long odontologoId) {
        return service.agenda(desde, dias, odontologoId);
    }

    @Operation(summary = "Huecos libres de un día",
            description = "Permiso: citas.crear o citas.reprogramar. Cada odontólogo por separado y sin la antelación "
                    + "mínima de la web. Al reprogramar, excluir_cita_id hace que la propia cita no cuente como ocupada.")
    @PreAuthorize("hasAnyAuthority('citas.crear', 'citas.reprogramar')")
    @GetMapping("/disponibilidad")
    public List<HuecoResponse> disponibilidad(
            @RequestParam("tratamiento_id") Long tratamientoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "odontologo_id", required = false) Long odontologoId,
            @RequestParam(name = "excluir_cita_id", required = false) Long excluirCitaId) {
        return service.huecos(tratamientoId, fecha, odontologoId, excluirCitaId);
    }
}
