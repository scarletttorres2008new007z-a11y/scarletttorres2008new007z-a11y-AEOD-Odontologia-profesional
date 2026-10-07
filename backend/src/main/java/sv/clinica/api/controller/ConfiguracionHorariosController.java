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
import sv.clinica.api.dto.HorarioClinicaRequest;
import sv.clinica.api.dto.HorariosGuardadosResponse;
import sv.clinica.api.dto.HorariosResponse;
import sv.clinica.api.dto.TurnosRequest;
import sv.clinica.api.service.ConfiguracionHorariosService;

/** Horario de la clínica y turnos de los odontólogos (permiso horarios.gestionar). */
@Tag(name = "Configuración de la agenda", description = "Odontólogos, tratamientos, horarios y bloqueos")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/configuracion/horarios")
@PreAuthorize("hasAuthority('horarios.gestionar')")
public class ConfiguracionHorariosController {

    private final ConfiguracionHorariosService service;

    public ConfiguracionHorariosController(ConfiguracionHorariosService service) {
        this.service = service;
    }

    @Operation(summary = "Horario de la clínica y turnos de cada odontólogo activo",
            description = "Permiso: horarios.gestionar.")
    @GetMapping
    public HorariosResponse obtener() {
        return service.obtener();
    }

    @Operation(summary = "Cambiar el horario de la clínica",
            description = "Permiso: horarios.gestionar. Sustituye toda la semana: el día que no se envía queda cerrado. "
                    + "Devuelve las citas que quedan fuera del horario nuevo (no se cancelan).")
    @PutMapping("/clinica")
    public HorariosGuardadosResponse cambiarClinica(@Valid @RequestBody HorarioClinicaRequest request) {
        return service.cambiarClinica(request);
    }

    @Operation(summary = "Cambiar los turnos de un odontólogo",
            description = "Permiso: horarios.gestionar. Sustituye toda su semana. Devuelve las citas que quedan fuera "
                    + "de sus turnos nuevos (no se cancelan).")
    @PutMapping("/odontologos/{id}")
    public HorariosGuardadosResponse cambiarTurnos(@PathVariable Long id, @Valid @RequestBody TurnosRequest request) {
        return service.cambiarTurnos(id, request);
    }
}
