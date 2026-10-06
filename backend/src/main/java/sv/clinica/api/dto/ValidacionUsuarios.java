package sv.clinica.api.dto;

/** Reglas comunes de los datos de usuario. */
public final class ValidacionUsuarios {

    /** Letras sin tildes, números, punto, guion y guion bajo (se guarda en minúsculas). */
    public static final String USERNAME = "^[A-Za-z0-9._-]+$";
    public static final String USERNAME_MENSAJE = "Usa solo letras sin tildes, números, punto, guion o guion bajo.";

    private ValidacionUsuarios() {
    }
}
