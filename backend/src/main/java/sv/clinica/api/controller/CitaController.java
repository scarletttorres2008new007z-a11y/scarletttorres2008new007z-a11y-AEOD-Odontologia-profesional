package sv.clinica.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.ApiResponse;
import sv.clinica.api.dto.CitaRequest;
import sv.clinica.api.dto.CitaResponse;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OrigenCita;
import sv.clinica.api.service.CitaService;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    /**
     * Reserva un horario elegido entre los de /api/disponibilidad.
     * 201 con la cita · 400 datos inválidos · 409 el horario ya no está libre.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse reservar(@Valid @RequestBody CitaRequest request) {
        Cita cita = service.reservar(request, OrigenCita.LANDING);
        String mensaje = cita.getEstado() == EstadoCita.CONFIRMADA
                ? "Tu cita está confirmada."
                : "Tu cita está reservada. Te llamaremos para confirmarla.";
        return ApiResponse.ok(mensaje, CitaResponse.from(cita));
    }
}
