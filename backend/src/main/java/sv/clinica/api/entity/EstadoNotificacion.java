package sv.clinica.api.entity;

public enum EstadoNotificacion {
    /** Guardada, a la espera de que exista un canal de envío (correo, WhatsApp, app…). */
    PENDIENTE,
    ENVIADA,
    ERROR,
    DESCARTADA
}
