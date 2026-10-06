package sv.clinica.api.security;

import java.util.Set;

/**
 * Quién hace la petición, comprobado en la base de datos en cada petición: sesión abierta, usuario activo
 * y permisos actuales. Así desactivar a alguien o quitarle un permiso tiene efecto al momento.
 */
public record UsuarioAutenticado(Long id, String username, String nombre, Long sesionId, Set<String> permisos) {

    public boolean tienePermiso(String permiso) {
        return permisos.contains(permiso);
    }
}
