package sv.clinica.api.entity;

/** Sobre qué tipo de registro se hizo una operación auditada. */
public enum EntidadAuditoria {
    USUARIO,
    ROL,
    CITA,
    PACIENTE,
    ODONTOLOGO,
    TRATAMIENTO,
    HORARIO_CLINICA,
    HORARIO_ODONTOLOGO,
    BLOQUEO
}
