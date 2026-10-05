package sv.clinica.landing.exception;

/** Se lanza cuando un recurso solicitado no existe o no está activo (responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
