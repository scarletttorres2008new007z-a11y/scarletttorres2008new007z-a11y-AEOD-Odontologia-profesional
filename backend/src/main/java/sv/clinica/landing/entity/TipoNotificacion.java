package sv.clinica.landing.entity;

public enum TipoNotificacion {
    NUEVA_CITA,
    CITA_CANCELADA,
    CITA_REPROGRAMADA,
    /** Se liberó un hueco que encaja con alguien de la lista de espera. */
    HUECO_LIBERADO
}
