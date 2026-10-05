package sv.clinica.landing.exception;

/** El horario elegido ya no está libre (otra persona lo reservó antes). Responde 409. */
public class HorarioNoDisponibleException extends RuntimeException {

    public static final String MENSAJE = "Este horario acaba de ser reservado. Selecciona otra opción.";

    public HorarioNoDisponibleException() {
        super(MENSAJE);
    }
}
