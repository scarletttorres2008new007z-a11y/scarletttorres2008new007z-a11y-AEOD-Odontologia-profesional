package sv.clinica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import sv.clinica.api.dto.ApiResponse;
import sv.clinica.api.dto.CancelacionPacienteRequest;
import sv.clinica.api.dto.CancelacionRequest;
import sv.clinica.api.dto.CitaDetalleResponse;
import sv.clinica.api.dto.CitaRequest;
import sv.clinica.api.dto.CitaResponse;
import sv.clinica.api.dto.CitaResumenResponse;
import sv.clinica.api.dto.EstadoCitaRequest;
import sv.clinica.api.dto.NotasCitaRequest;
import sv.clinica.api.dto.PacienteCitaRequest;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.dto.ReprogramacionRequest;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OrigenCita;
import sv.clinica.api.service.AgendaService;
import sv.clinica.api.service.BusquedaDeCitas;
import sv.clinica.api.service.CitaService;

import java.time.LocalDate;

/**
 * Citas. Hay una sola tabla de citas para todos: la landing reserva y cancela por aquí sin sesión (con su código)
 * y el software consulta y gestiona las mismas citas con sesión y permisos.
 * Las citas que da la clínica se crean en POST /api/pacientes/{id}/citas, porque siempre son de un paciente con ficha.
 */
@Tag(name = "Citas", description = "Reservas de la web y gestión de citas desde el software")
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService service;
    private final AgendaService agenda;

    public CitaController(CitaService service, AgendaService agenda) {
        this.service = service;
        this.agenda = agenda;
    }

    /* ─────────────── Landing (sin sesión) ─────────────── */

    /**
     * Reserva un horario elegido entre los de /api/disponibilidad.
     * 201 con la cita · 400 datos inválidos · 409 el horario ya no está libre.
     */
    @Operation(summary = "Reservar una cita desde la web", description = "Público. Entra PENDIENTE hasta que la clínica la confirma.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse reservar(@Valid @RequestBody CitaRequest request) {
        Cita cita = service.reservar(request, OrigenCita.LANDING);
        String mensaje = cita.getEstado() == EstadoCita.CONFIRMADA
                ? "Tu cita está confirmada."
                : "Tu cita está reservada. Te llamaremos para confirmarla.";
        return ApiResponse.ok(mensaje, CitaResponse.from(cita));
    }

    @Operation(summary = "Cancelar una cita desde la web",
            description = "Público. El paciente se identifica con el código que recibió al reservar y debe hacerlo con "
                    + "la antelación mínima (409 si no). El hueco queda libre al momento.")
    @PostMapping("/cancelacion")
    public ApiResponse cancelarComoPaciente(@Valid @RequestBody CancelacionPacienteRequest request) {
        CitaResponse cita = service.cancelarComoPaciente(request.codigo().trim(), OrigenCita.LANDING,
                "Cancelada por el paciente desde la web");
        return ApiResponse.ok("Tu cita se ha cancelado. Ese horario vuelve a estar libre.", cita);
    }

    /* ─────────────── Software (con sesión y permiso) ─────────────── */

    @Operation(summary = "Buscar citas",
            description = "Permiso: citas.ver. Sin citas.ver_todas, solo las del odontólogo vinculado al usuario.")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAuthority('citas.ver')")
    @GetMapping
    public PaginaResponse<CitaResumenResponse> buscar(
            @Parameter(description = "Desde este día (incluido)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Hasta este día (incluido)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "odontologo_id", required = false) Long odontologoId,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) OrigenCita origen,
            @RequestParam(name = "paciente_id", required = false) Long pacienteId,
            @Parameter(description = "true = solo las que aún no están vinculadas a la ficha de un paciente")
            @RequestParam(name = "sin_ficha", required = false) Boolean sinFicha,
            @Parameter(description = "true = de la más reciente a la más antigua") @RequestParam(defaultValue = "false")
            boolean recientes,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return agenda.buscar(new BusquedaDeCitas(desde, hasta, odontologoId, estado, origen, pacienteId, sinFicha),
                recientes, Paginas.pagina(pagina), Paginas.tamano(tamano));
    }

    @Operation(summary = "Ver una cita", description = "Permiso: citas.ver. 404 si no existe o es de otra agenda.")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAuthority('citas.ver')")
    @GetMapping("/{id}")
    public CitaDetalleResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Cambiar el estado",
            description = "Permiso: citas.cambiar_estado. Confirmar, en consulta, completada o no asistió; los estados "
                    + "posibles en cada momento vienen en estados_siguientes.")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAuthority('citas.cambiar_estado')")
    @PutMapping("/{id}/estado")
    public CitaDetalleResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoCitaRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @Operation(summary = "Cancelar", description = "Permiso: citas.cancelar. No se borra: queda CANCELADA y su hueco libre.")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAuthority('citas.cancelar')")
    @PostMapping("/{id}/cancelacion")
    public CitaDetalleResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelacionRequest request) {
        return service.cancelarDesdeLaClinica(id, request.motivo());
    }

    @Operation(summary = "Reprogramar",
            description = "Permiso: citas.reprogramar. Devuelve la cita nueva; la anterior queda REPROGRAMADA. "
                    + "409 si el nuevo horario ya no está libre (entonces no cambia nada).")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAuthority('citas.reprogramar')")
    @PostMapping("/{id}/reprogramacion")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaDetalleResponse reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramacionRequest request) {
        return service.reprogramarDesdeLaClinica(id, request);
    }

    @Operation(summary = "Vincular a la ficha de un paciente", description = "Permiso: citas.editar.")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAuthority('citas.editar')")
    @PutMapping("/{id}/paciente")
    public CitaDetalleResponse vincularPaciente(@PathVariable Long id, @Valid @RequestBody PacienteCitaRequest request) {
        return service.vincularPaciente(id, request.pacienteId());
    }

    @Operation(summary = "Cambiar las notas internas", description = "Permiso: citas.editar.")
    @SecurityRequirement(name = "bearer")
    @PreAuthorize("hasAuthority('citas.editar')")
    @PutMapping("/{id}/notas")
    public CitaDetalleResponse cambiarNotas(@PathVariable Long id, @Valid @RequestBody NotasCitaRequest request) {
        return service.cambiarNotas(id, request.notasInternas());
    }
}
