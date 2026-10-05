package sv.clinica.landing.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sv.clinica.landing.dto.DisponibilidadResponse;
import sv.clinica.landing.entity.Franja;
import sv.clinica.landing.service.DisponibilidadService;

import java.time.LocalDate;

/** Horarios disponibles. El cálculo es siempre del backend; la landing solo los muestra. */
@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService service;

    public DisponibilidadController(DisponibilidadService service) {
        this.service = service;
    }

    /** GET /api/disponibilidad?tratamiento_id=2&fecha=2026-10-15[&odontologo_id=3][&franja=MANANA|TARDE] */
    @GetMapping
    public DisponibilidadResponse dia(
            @RequestParam("tratamiento_id") Long tratamientoId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "odontologo_id", required = false) Long odontologoId,
            @RequestParam(name = "franja", required = false) Franja franja) {
        return service.consultarDia(tratamientoId, fecha, odontologoId, franja);
    }

    /** GET /api/disponibilidad/proximos?tratamiento_id=2[&desde=…][&odontologo_id=…][&franja=…][&limite=6] */
    @GetMapping("/proximos")
    public DisponibilidadResponse proximos(
            @RequestParam("tratamiento_id") Long tratamientoId,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "odontologo_id", required = false) Long odontologoId,
            @RequestParam(name = "franja", required = false) Franja franja,
            @RequestParam(name = "limite", defaultValue = "6") int limite) {
        return service.consultarProximos(tratamientoId, desde, odontologoId, franja, limite);
    }
}
