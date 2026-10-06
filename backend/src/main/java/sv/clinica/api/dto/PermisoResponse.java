package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Permiso;

@Schema(requiredProperties = {"id", "codigo", "modulo", "descripcion"})
public record PermisoResponse(Long id, String codigo, String modulo, String descripcion) {

    public static PermisoResponse from(Permiso p) {
        return new PermisoResponse(p.getId(), p.getCodigo(), p.getModulo(), p.getDescripcion());
    }
}
