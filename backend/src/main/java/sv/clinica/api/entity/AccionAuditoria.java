package sv.clinica.api.entity;

/** Operaciones que quedan en la auditoría. Se guardan como texto: añadir una nueva no necesita migración. */
public enum AccionAuditoria {
    INICIAR_SESION,
    INICIO_SESION_FALLIDO,
    BLOQUEAR_POR_INTENTOS,
    CERRAR_SESION,
    CAMBIAR_PASSWORD,
    RESTABLECER_PASSWORD,
    CREAR,
    EDITAR,
    ACTIVAR,
    DESACTIVAR,
    CAMBIAR_ROLES,
    CAMBIAR_PERMISOS,
    RESERVAR,
    CANCELAR,
    REPROGRAMAR
}
