package sv.clinica.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.api.dto.OdontologoResponse;
import sv.clinica.api.service.OdontologoService;

import java.util.List;

@RestController
@RequestMapping("/api/odontologos")
public class OdontologoController {

    private final OdontologoService service;

    public OdontologoController(OdontologoService service) {
        this.service = service;
    }

    @GetMapping
    public List<OdontologoResponse> listar() {
        return service.listarActivos();
    }

    @GetMapping("/{id}")
    public OdontologoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
