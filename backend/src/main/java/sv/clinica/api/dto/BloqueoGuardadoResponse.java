package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"bloqueo", "citas_afectadas"})
public record BloqueoGuardadoResponse(BloqueoResponse bloqueo, CitasAfectadasResponse citasAfectadas) {
}
