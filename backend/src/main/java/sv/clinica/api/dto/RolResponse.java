package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Permiso;
import sv.clinica.api.entity.Rol;

import java.util.Comparator;
import java.util.List;

@Schema(description = "Rol con sus permisos",
        requiredProperties = {"id", "codigo", "nombre", "editable", "usuarios", "permisos"})
public record RolResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        @Schema(description = "false en el administrador: tiene siempre todos los permisos") boolean editable,
        @Schema(description = "Usuarios que tienen este rol") long usuarios,
        @Schema(description = "Códigos de sus permisos") List<String> permisos) {

    public static RolResponse from(Rol r, long usuarios) {
        return new RolResponse(r.getId(), r.getCodigo(), r.getNombre(), r.getDescripcion(), !r.esAdministrador(),
                usuarios, r.getPermisos().stream().sorted(Comparator.comparing(Permiso::getId)).map(Permiso::getCodigo).toList());
    }
}
