package sv.clinica.landing.dto;

import sv.clinica.landing.entity.Tratamiento;

import java.math.BigDecimal;

public record TratamientoResponse(
        Long id,
        String nombre,
        String descripcion,
        String descripcionCorta,
        BigDecimal precioDesde,
        String duracionAproximada,
        String imagen) {

    public static TratamientoResponse from(Tratamiento t) {
        return new TratamientoResponse(t.getId(), t.getNombre(), t.getDescripcion(), t.getDescripcionCorta(),
                t.getPrecioDesde(), t.getDuracionAproximada(), t.getImagen());
    }
}
