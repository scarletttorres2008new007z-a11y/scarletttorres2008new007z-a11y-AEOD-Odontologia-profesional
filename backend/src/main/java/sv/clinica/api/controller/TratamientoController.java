package sv.clinica.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.TratamientoResponse;
import sv.clinica.api.service.TratamientoService;

import java.util.List;

@RestController
@RequestMapping("/api/tratamientos")
public class TratamientoController {

    private final TratamientoService service;

    public TratamientoController(TratamientoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TratamientoResponse> listar() {
        return service.listarActivos();
    }

    @GetMapping("/{id}")
    public TratamientoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
