package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Usuario;

import java.util.Collection;
import java.util.List;

/** La persona que ha iniciado sesión, con lo que puede hacer. El software decide qué mostrar con "permisos". */
@Schema(description = "Usuario con sesión iniciada",
        requiredProperties = {"id", "username", "email", "nombre", "roles", "permisos"})
public record UsuarioActualResponse(
        Long id,
        String username,
        String email,
        String nombre,
        List<RolResumen> roles,
        @Schema(description = "Códigos de los permisos que suman sus roles", example = "[\"usuarios.ver\", \"auditoria.ver\"]")
        List<String> permisos) {

    public static UsuarioActualResponse from(Usuario u, Collection<String> permisos) {
        return new UsuarioActualResponse(u.getId(), u.getUsername(), u.getEmail(), u.getNombre(),
                UsuarioResponse.roles(u), permisos.stream().sorted().toList());
    }
}
