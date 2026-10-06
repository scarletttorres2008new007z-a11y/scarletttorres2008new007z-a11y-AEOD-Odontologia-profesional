package sv.clinica.api.entity;

/** Desde dónde se hizo una operación registrada en la auditoría. */
public enum OrigenAuditoria {
    /** Un paciente desde la web pública. */
    LANDING,
    /** Una persona del equipo desde el software de gestión. */
    SOFTWARE,
    /** Futura app del paciente. */
    APP,
    /** El propio backend (por ejemplo, el administrador inicial). */
    SISTEMA
}
