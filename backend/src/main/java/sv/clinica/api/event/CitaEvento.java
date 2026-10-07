package sv.clinica.api.event;

import sv.clinica.api.entity.EstadoCita;

/**
 * Algo cambió en la agenda. Se publica dentro de la transacción de la cita, así quien lo escucha
 * (la auditoría, la bandeja de notificaciones y la lista de espera) guarda sus datos en la misma transacción.
 *
 * @param citaId          la cita afectada (en una reprogramación, la nueva)
 * @param citaAnteriorId  solo en REPROGRAMADA: la cita que se movió y cuyo hueco quedó libre
 * @param estadoAnterior  solo en CANCELADA: el estado que tenía antes de cancelarse
 */
public record CitaEvento(Tipo tipo, Long citaId, Long citaAnteriorId, EstadoCita estadoAnterior) {

    public enum Tipo {
        RESERVADA,
        CANCELADA,
        REPROGRAMADA
    }

    public static CitaEvento reservada(Long citaId) {
        return new CitaEvento(Tipo.RESERVADA, citaId, null, null);
    }

    public static CitaEvento cancelada(Long citaId, EstadoCita estadoAnterior) {
        return new CitaEvento(Tipo.CANCELADA, citaId, null, estadoAnterior);
    }

    public static CitaEvento reprogramada(Long nuevaId, Long anteriorId) {
        return new CitaEvento(Tipo.REPROGRAMADA, nuevaId, anteriorId, null);
    }
}
