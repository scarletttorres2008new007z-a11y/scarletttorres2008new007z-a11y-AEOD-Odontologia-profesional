package sv.clinica.api.exception;

/**
 * La operación choca con el estado actual de los datos: un usuario o correo que ya existe,
 * quitar el último administrador, cambiar los permisos del administrador… Responde 409.
 * Si se indica un campo, el error se muestra junto a ese campo del formulario.
 */
public class ConflictoException extends RuntimeException {

    private final String campo;

    public ConflictoException(String message) {
        this(null, message);
    }

    public ConflictoException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
