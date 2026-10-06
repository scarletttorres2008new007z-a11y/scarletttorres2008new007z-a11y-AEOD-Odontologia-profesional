package sv.clinica.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.ApiResponse;
import sv.clinica.api.dto.ContactoRequest;
import sv.clinica.api.service.ContactoService;

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
