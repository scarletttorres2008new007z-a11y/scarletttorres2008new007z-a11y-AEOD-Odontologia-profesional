package sv.clinica.api.entity;

public enum EstadoListaEspera {
    ACTIVA,
    /** Se le avisó de un hueco liberado. */
    AVISADA,
    /** Consiguió cita. */
    ATENDIDA,
    CANCELADA
}
