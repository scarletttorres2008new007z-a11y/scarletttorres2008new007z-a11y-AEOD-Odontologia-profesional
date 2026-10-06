package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"horarios", "citas_afectadas"})
public record HorariosGuardadosResponse(HorariosResponse horarios, CitasAfectadasResponse citasAfectadas) {
}
