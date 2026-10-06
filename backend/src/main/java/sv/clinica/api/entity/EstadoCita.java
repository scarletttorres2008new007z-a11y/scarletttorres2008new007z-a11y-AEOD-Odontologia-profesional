package sv.clinica.api.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados de una cita. Una cita nunca se borra: cambia de estado.
 * Solo cancelar y reprogramar liberan su hueco en la agenda; los demás estados lo siguen ocupando.
 */
public enum EstadoCita {
    /** Reservada (el hueco está ocupado) y pendiente de que la clínica la confirme. Así entran las de la web. */
    PENDIENTE,
    CONFIRMADA,
    /** El paciente está en la consulta. */
    EN_ATENCION,
    COMPLETADA,
    NO_ASISTIO,
    /** Cancelada: el hueco vuelve a estar disponible. */
    CANCELADA,
    /** Movida a otra cita (enlazada con cita_anterior_id). El hueco antiguo queda libre. */
    REPROGRAMADA;

    public static final Set<EstadoCita> OCUPAN_AGENDA = EnumSet.complementOf(EnumSet.of(CANCELADA, REPROGRAMADA));

    /** Solo una cita que todavía no ha llegado se puede cancelar o mover. */
    public static final Set<EstadoCita> MODIFICABLES = EnumSet.of(PENDIENTE, CONFIRMADA);

    /** Estados que se marcan el día de la cita (o después), no antes. */
    public static final Set<EstadoCita> DEL_DIA = EnumSet.of(EN_ATENCION, COMPLETADA, NO_ASISTIO);

    /** A qué estados se puede pasar desde cada uno con «cambiar estado» (cancelar y reprogramar van aparte). */
    private static final Map<EstadoCita, Set<EstadoCita>> SIGUIENTES = Map.of(
            PENDIENTE, EnumSet.of(CONFIRMADA, EN_ATENCION, NO_ASISTIO),
            CONFIRMADA, EnumSet.of(EN_ATENCION, NO_ASISTIO),
            EN_ATENCION, EnumSet.of(COMPLETADA),
            // Por si se marcó por error: vuelve a confirmada sin perder su hueco
            NO_ASISTIO, EnumSet.of(CONFIRMADA));

    public boolean ocupaAgenda() {
        return OCUPAN_AGENDA.contains(this);
    }

    public boolean esModificable() {
        return MODIFICABLES.contains(this);
    }

    public Set<EstadoCita> siguientes() {
        return SIGUIENTES.getOrDefault(this, EnumSet.noneOf(EstadoCita.class));
    }
}
