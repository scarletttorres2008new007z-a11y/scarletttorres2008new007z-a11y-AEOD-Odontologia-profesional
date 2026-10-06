package sv.clinica.api.security;

/**
 * Códigos de los permisos que comprueba el backend. Son los mismos que crean las migraciones (V2, V3…) en la tabla permisos;
 * en los @PreAuthorize de los controllers se escriben tal cual ("hasAuthority('usuarios.ver')").
 */
public final class Permisos {

    public static final String USUARIOS_VER = "usuarios.ver";
    public static final String USUARIOS_CREAR = "usuarios.crear";
    public static final String USUARIOS_EDITAR = "usuarios.editar";
    public static final String USUARIOS_ASIGNAR_ROLES = "usuarios.asignar_roles";
    public static final String ROLES_VER = "roles.ver";
    public static final String ROLES_EDITAR = "roles.editar";
    public static final String AUDITORIA_VER = "auditoria.ver";
    public static final String PACIENTES_VER = "pacientes.ver";
    public static final String PACIENTES_CREAR = "pacientes.crear";
    public static final String PACIENTES_EDITAR = "pacientes.editar";

    private Permisos() {
    }
}
