package sv.clinica.landing.exception;

/**
 * Datos que pasan las anotaciones de validación pero no las reglas del negocio
 * (tratamiento inexistente, fecha pasada, hora fuera de horario). Responde 400.
 */
public class DatosInvalidosException extends RuntimeException {

    private final String campo;

    public DatosInvalidosException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
