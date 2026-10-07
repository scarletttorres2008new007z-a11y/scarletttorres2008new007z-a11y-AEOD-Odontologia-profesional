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
    public static final String CITAS_VER = "citas.ver";
    public static final String CITAS_VER_TODAS = "citas.ver_todas";
    public static final String CITAS_CREAR = "citas.crear";
    public static final String CITAS_CAMBIAR_ESTADO = "citas.cambiar_estado";
    public static final String CITAS_CANCELAR = "citas.cancelar";
    public static final String CITAS_REPROGRAMAR = "citas.reprogramar";
    public static final String CITAS_EDITAR = "citas.editar";
    public static final String ODONTOLOGOS_GESTIONAR = "odontologos.gestionar";
    public static final String TRATAMIENTOS_GESTIONAR = "tratamientos.gestionar";
    public static final String HORARIOS_GESTIONAR = "horarios.gestionar";
    public static final String BLOQUEOS_GESTIONAR = "bloqueos.gestionar";

    private Permisos() {
    }
}
