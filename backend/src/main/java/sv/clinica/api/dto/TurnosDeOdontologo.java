package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"id", "nombre", "turnos"})
public record TurnosDeOdontologo(Long id, String nombre, List<TurnoSemanal> turnos) {
}
