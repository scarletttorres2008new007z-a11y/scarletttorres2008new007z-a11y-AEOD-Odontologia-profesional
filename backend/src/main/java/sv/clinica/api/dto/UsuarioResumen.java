package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Usuario;

@Schema(requiredProperties = {"id", "username", "nombre"})
public record UsuarioResumen(Long id, String username, String nombre) {

    public static UsuarioResumen from(Usuario u) {
        return new UsuarioResumen(u.getId(), u.getUsername(), u.getNombre());
    }
}
