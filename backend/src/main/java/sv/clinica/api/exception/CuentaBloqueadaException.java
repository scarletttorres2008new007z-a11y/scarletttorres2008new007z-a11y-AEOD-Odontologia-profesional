package sv.clinica.api.exception;

/** Demasiadas contraseñas incorrectas seguidas: la cuenta queda bloqueada un tiempo. Responde 423. */
public class CuentaBloqueadaException extends RuntimeException {

    public CuentaBloqueadaException(long minutos) {
        super("Demasiados intentos fallidos. Por seguridad, la cuenta queda bloqueada " + minutos
                + (minutos == 1 ? " minuto." : " minutos."));
    }
}
