package sv.clinica.api.service;

import sv.clinica.api.exception.DatosInvalidosException;

import java.nio.charset.StandardCharsets;

/** Requisitos de una contraseña nueva. Se aplican al crear un usuario, al cambiarla y al restablecerla. */
public final class PoliticaDePasswords {

    public static final int LONGITUD_MINIMA = 10;
    /** BCrypt solo tiene en cuenta los primeros 72 bytes: una contraseña más larga no se acepta. */
    public static final int BYTES_MAXIMOS = 72;

    private PoliticaDePasswords() {
    }

    /** @throws DatosInvalidosException (400) con el motivo, asociado al campo indicado */
    public static void comprobar(String campo, String password, String username, String email) {
        if (password == null || password.isBlank() || password.length() < LONGITUD_MINIMA) {
            throw new DatosInvalidosException(campo, "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        if (demasiadoLarga(password)) {
            throw new DatosInvalidosException(campo, "La contraseña es demasiado larga (máximo " + BYTES_MAXIMOS + " caracteres).");
        }
        if (password.equalsIgnoreCase(username) || password.equalsIgnoreCase(email)) {
            throw new DatosInvalidosException(campo, "La contraseña no puede ser igual al usuario ni al correo.");
        }
    }

    public static boolean demasiadoLarga(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS;
    }
}
