package sv.clinica.landing.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una cita. Una cita nunca se borra: cambia de estado.
 * Solo PENDIENTE y CONFIRMADA ocupan su hueco en la agenda.
 */
public enum EstadoCita {
    /** Reservada (el hueco está ocupado) y pendiente de que la clínica la confirme. */
    PENDIENTE,
    CONFIRMADA,
    /** Cancelada: el hueco vuelve a estar disponible. */
    CANCELADA,
    /** Movida a otra cita (enlazada con cita_anterior_id). El hueco antiguo queda libre. */
    REPROGRAMADA,
    /** Para el futuro software de clínica. */
    COMPLETADA,
    /** Para el futuro software de clínica. */
    NO_ASISTIO;

    public static final Set<EstadoCita> OCUPAN_AGENDA = EnumSet.of(PENDIENTE, CONFIRMADA);

    public boolean ocupaAgenda() {
        return OCUPAN_AGENDA.contains(this);
    }
}
