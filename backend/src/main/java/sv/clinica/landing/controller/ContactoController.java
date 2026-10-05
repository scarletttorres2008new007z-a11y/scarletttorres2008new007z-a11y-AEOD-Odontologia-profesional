package sv.clinica.landing.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.landing.dto.ApiResponse;
import sv.clinica.landing.dto.ContactoRequest;
import sv.clinica.landing.service.ContactoService;

@RestController
@RequestMapping("/api/contacto")
public class ContactoController {

    private final ContactoService service;

    public ContactoController(ContactoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse enviar(@Valid @RequestBody ContactoRequest request) {
        service.registrar(request);
        return ApiResponse.ok("Solicitud enviada correctamente.");
    }
}
