package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Citas pendientes o confirmadas, de hoy en adelante, que quedan fuera del horario o dentro de un bloqueo después de
 * un cambio. No se cancelan solas: la clínica decide si moverlas o cancelarlas.
 *
 * @param citas las citas (como mucho 50); solo llegan si quien hace el cambio puede ver todas las agendas
 */
@Schema(requiredProperties = {"total"})
public record CitasAfectadasResponse(int total, List<CitaResumenResponse> citas) {
}
