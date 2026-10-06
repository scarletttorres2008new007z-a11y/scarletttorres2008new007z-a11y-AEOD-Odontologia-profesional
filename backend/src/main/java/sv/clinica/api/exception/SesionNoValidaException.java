package sv.clinica.api.exception;

/** La sesión no existe, caducó o se cerró. Responde 401: hay que volver a iniciar sesión. */
public class SesionNoValidaException extends RuntimeException {

    public static final String MENSAJE = "Tu sesión ha terminado. Vuelve a iniciar sesión.";

    public SesionNoValidaException() {
        super(MENSAJE);
    }
}
