package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Odontologo;

@Schema(requiredProperties = {"id", "nombre"})
public record OdontologoResponse(
        Long id,
        String nombre,
        String especialidad,
        String descripcion,
        String imagen) {

    public static OdontologoResponse from(Odontologo o) {
        return new OdontologoResponse(o.getId(), o.getNombre(), o.getEspecialidad(), o.getDescripcion(), o.getImagen());
    }
}
