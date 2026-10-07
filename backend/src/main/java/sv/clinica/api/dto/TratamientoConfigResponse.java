package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.Tratamiento;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Tratamiento tal y como se configura en el software (también los desactivados).
 *
 * @param duracionMinutos minutos que ocupa la cita en la agenda
 * @param odontologos     quién lo hace; vacío = cualquier odontólogo
 */
@Schema(requiredProperties = {"id", "nombre", "activo", "odontologos"})
public record TratamientoConfigResponse(
        Long id,
        String nombre,
        String descripcionCorta,
        BigDecimal precioDesde,
        String duracionAproximada,
        Integer duracionMinutos,
        boolean activo,
        List<OdontologoAsignado> odontologos) {

    /** Odontólogo que hace el tratamiento; puede estar desactivado (entonces no tiene huecos). */
    @Schema(requiredProperties = {"id", "nombre", "activo"})
    public record OdontologoAsignado(Long id, String nombre, boolean activo) {
    }

    public static TratamientoConfigResponse from(Tratamiento t) {
        return new TratamientoConfigResponse(t.getId(), t.getNombre(), t.getDescripcionCorta(), t.getPrecioDesde(),
                t.getDuracionAproximada(), t.getDuracionMinutos(), t.isActivo(),
                t.getOdontologos().stream()
                        .sorted(Comparator.comparing(Odontologo::getOrden).thenComparing(Odontologo::getId))
                        .map(o -> new OdontologoAsignado(o.getId(), o.getNombre(), o.isActivo()))
                        .toList());
    }
}
