package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Odontologo;

import java.util.List;

/**
 * Odontólogo tal y como se configura en el software (también los desactivados).
 *
 * @param usuario      su usuario del software, con el que ve su propia agenda; vacío si no tiene
 * @param tratamientos tratamientos activos que puede hacer (los que hace cualquiera y los que tiene asignados)
 */
@Schema(requiredProperties = {"id", "nombre", "activo", "tratamientos"})
public record OdontologoConfigResponse(
        Long id,
        String nombre,
        String especialidad,
        String descripcion,
        boolean activo,
        UsuarioResumen usuario,
        List<String> tratamientos) {

    public static OdontologoConfigResponse from(Odontologo o, List<String> tratamientos) {
        return new OdontologoConfigResponse(o.getId(), o.getNombre(), o.getEspecialidad(), o.getDescripcion(),
                o.isActivo(), o.getUsuario() == null ? null : UsuarioResumen.from(o.getUsuario()), tratamientos);
    }
}
