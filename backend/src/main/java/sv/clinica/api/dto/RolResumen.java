package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Rol;

@Schema(requiredProperties = {"codigo", "nombre"})
public record RolResumen(String codigo, String nombre) {

    public static RolResumen from(Rol r) {
        return new RolResumen(r.getCodigo(), r.getNombre());
    }
}
