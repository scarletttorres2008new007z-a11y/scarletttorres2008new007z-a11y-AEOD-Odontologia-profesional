package sv.clinica.landing.dto;

import sv.clinica.landing.entity.Odontologo;
import sv.clinica.landing.entity.Tratamiento;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param duracionMinutos minutos de la cita que se reserva online
 * @param odontologoIds   odontólogos que lo realizan; vacío = cualquiera
 */
public record TratamientoResponse(
        Long id,
        String nombre,
        String descripcion,
        String descripcionCorta,
        BigDecimal precioDesde,
        String duracionAproximada,
        Integer duracionMinutos,
        String imagen,
        List<Long> odontologoIds) {

    public static TratamientoResponse from(Tratamiento t) {
        return new TratamientoResponse(t.getId(), t.getNombre(), t.getDescripcion(), t.getDescripcionCorta(),
                t.getPrecioDesde(), t.getDuracionAproximada(), t.getDuracionMinutos(), t.getImagen(),
                t.getOdontologos().stream().map(Odontologo::getId).sorted().toList());
    }
}
