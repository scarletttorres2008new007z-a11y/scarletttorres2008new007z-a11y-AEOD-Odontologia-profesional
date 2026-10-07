package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.Rol;
import sv.clinica.api.entity.Usuario;

import java.util.List;

/**
 * Usuario activo que se puede vincular a un odontólogo.
 *
 * @param odontologoId odontólogo al que ya está vinculado; vacío si a ninguno
 */
@Schema(requiredProperties = {"id", "username", "nombre", "roles"})
public record UsuarioParaOdontologoResponse(
        Long id,
        String username,
        String nombre,
        List<String> roles,
        Long odontologoId,
        String odontologo) {

    public static UsuarioParaOdontologoResponse from(Usuario u, Odontologo vinculado) {
        return new UsuarioParaOdontologoResponse(u.getId(), u.getUsername(), u.getNombre(),
                u.getRoles().stream().map(Rol::getNombre).sorted().toList(),
                vinculado == null ? null : vinculado.getId(), vinculado == null ? null : vinculado.getNombre());
    }
}
