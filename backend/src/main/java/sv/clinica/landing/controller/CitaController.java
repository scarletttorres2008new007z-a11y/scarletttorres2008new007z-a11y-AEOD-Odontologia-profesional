package sv.clinica.landing.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.landing.dto.ApiResponse;
import sv.clinica.landing.dto.CitaRequest;
import sv.clinica.landing.service.CitaService;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    /** Registra la preferencia de cita. La cita queda PENDIENTE hasta que la clínica la confirme. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse solicitar(@Valid @RequestBody CitaRequest request) {
        service.registrar(request);
        return ApiResponse.ok("Solicitud enviada correctamente. Te contactaremos para confirmar la cita.");
    }
}
