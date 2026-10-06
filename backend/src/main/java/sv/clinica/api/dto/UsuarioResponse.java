package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Rol;
import sv.clinica.api.entity.Usuario;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Schema(description = "Usuario del software de gestión. Las fechas están en hora de Madrid.",
        requiredProperties = {"id", "username", "email", "nombre", "activo", "bloqueado", "creado_en", "roles"})
public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String nombre,
        boolean activo,
        @Schema(description = "Bloqueado temporalmente por demasiados intentos fallidos") boolean bloqueado,
        LocalDateTime bloqueadoHasta,
        LocalDateTime ultimoAcceso,
        LocalDateTime creadoEn,
        List<RolResumen> roles) {

    public static UsuarioResponse from(Usuario u, LocalDateTime ahora) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.getNombre(), u.isActivo(),
                u.estaBloqueado(ahora), u.estaBloqueado(ahora) ? u.getBloqueadoHasta() : null,
                u.getUltimoAcceso(), u.getCreadoEn(), roles(u));
    }

    /** Roles del usuario en el orden en que se crearon (administrador primero). */
    static List<RolResumen> roles(Usuario u) {
        return u.getRoles().stream().sorted(Comparator.comparing(Rol::getId)).map(RolResumen::from).toList();
    }
}
