package sv.clinica.api.exception;

/** La cita no se puede cancelar o reprogramar (estado o antelación). Responde 409. */
public class CitaNoModificableException extends RuntimeException {

    public CitaNoModificableException(String message) {
        super(message);
    }
}
