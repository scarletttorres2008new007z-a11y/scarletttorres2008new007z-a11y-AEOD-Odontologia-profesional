package sv.clinica.api.dto;

import sv.clinica.api.entity.Odontologo;

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
