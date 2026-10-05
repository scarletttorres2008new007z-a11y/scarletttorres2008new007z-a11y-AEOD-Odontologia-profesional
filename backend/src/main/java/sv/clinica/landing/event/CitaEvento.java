package sv.clinica.landing.event;

/**
 * Algo cambió en la agenda. Se publica dentro de la transacción de la cita, así quien lo escucha
 * (hoy, la bandeja de notificaciones y la lista de espera) guarda sus datos en la misma transacción.
 *
 * @param citaId          la cita afectada (en una reprogramación, la nueva)
 * @param citaAnteriorId  solo en REPROGRAMADA: la cita que se movió y cuyo hueco quedó libre
 */
public record CitaEvento(Tipo tipo, Long citaId, Long citaAnteriorId) {

    public enum Tipo {
        RESERVADA,
        CANCELADA,
        REPROGRAMADA
    }

    public static CitaEvento reservada(Long citaId) {
        return new CitaEvento(Tipo.RESERVADA, citaId, null);
    }

    public static CitaEvento cancelada(Long citaId) {
        return new CitaEvento(Tipo.CANCELADA, citaId, null);
    }

    public static CitaEvento reprogramada(Long nuevaId, Long anteriorId) {
        return new CitaEvento(Tipo.REPROGRAMADA, nuevaId, anteriorId);
    }
}
