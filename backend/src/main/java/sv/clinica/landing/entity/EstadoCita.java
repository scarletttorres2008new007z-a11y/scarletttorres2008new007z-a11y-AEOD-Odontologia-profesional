package sv.clinica.landing.entity;

/**
 * Estados de una solicitud de cita.
 * PENDIENTE indica que la persona solo ha enviado su preferencia:
 * la cita no está confirmada hasta que la clínica la contacta.
 */
public enum EstadoCita {
    PENDIENTE,
    CONTACTADO,
    CONFIRMADO,
    CANCELADO
}
