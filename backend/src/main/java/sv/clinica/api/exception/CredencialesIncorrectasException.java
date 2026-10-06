package sv.clinica.api.exception;

/** Usuario o contraseña incorrectos, o usuario desactivado. Responde 401. */
public class CredencialesIncorrectasException extends RuntimeException {

    public static final String MENSAJE = "Usuario o contraseña incorrectos.";

    public CredencialesIncorrectasException(String message) {
        super(message);
    }
}
