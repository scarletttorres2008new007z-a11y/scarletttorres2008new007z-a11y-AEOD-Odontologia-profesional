package sv.clinica.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.OrigenAuditoria;

import java.time.LocalDateTime;

@Schema(description = "Operación registrada en la auditoría. La fecha está en hora de Madrid.",
        requiredProperties = {"id", "creado_en", "origen", "accion", "entidad"})
public record AuditoriaResponse(
        Long id,
        LocalDateTime creadoEn,
        @Schema(description = "Quién lo hizo; vacío si fue un paciente desde la web o el propio sistema") UsuarioResumen usuario,
        OrigenAuditoria origen,
        AccionAuditoria accion,
        EntidadAuditoria entidad,
        String entidadId,
        @Schema(description = "Datos antes del cambio", type = "object") JsonNode valorAnterior,
        @Schema(description = "Datos después del cambio", type = "object") JsonNode valorNuevo,
        String ip) {
}
