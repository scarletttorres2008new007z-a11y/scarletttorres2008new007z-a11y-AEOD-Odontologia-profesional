package sv.clinica.api.dto;

import sv.clinica.api.entity.TipoDocumento;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de los datos de un paciente. Las expresiones las usan las anotaciones de PacienteRequest; la comprobación
 * del documento la hace PacienteService, porque depende de dos campos a la vez (tipo y número).
 */
public final class ValidacionPacientes {

    /** Letras (con tildes y ñ), espacios, apóstrofo, punto y guion. Admite espacios alrededor (se quitan al guardar). */
    static final String NOMBRE = "^\\s*\\p{L}[\\p{L} '.-]*$";
    static final String NOMBRE_MENSAJE = "Usa solo letras, espacios, apóstrofos y guiones.";

    /** Contacto de emergencia: como un nombre, y además la relación entre paréntesis ("Ana Pérez (madre)"). */
    static final String CONTACTO_OPCIONAL = "^\\s*(?:\\p{L}[\\p{L} '.,()/-]*)?$";
    static final String CONTACTO_MENSAJE = "Escribe su nombre y, si quieres, la relación entre paréntesis.";

    /** Entre 9 y 15 dígitos; admite + inicial, espacios, paréntesis, puntos y guiones. */
    static final String TELEFONO = "^\\s*\\+?(?:[ ().-]*\\d){9,15}[ ().-]*$";
    static final String TELEFONO_OPCIONAL = "^\\s*(?:\\+?(?:[ ().-]*\\d){9,15}[ ().-]*)?$";
    static final String TELEFONO_MENSAJE = "El teléfono debe tener entre 9 y 15 dígitos (se admite el prefijo +).";

    /** Correo opcional: vacío o con dominio con punto (nombre@dominio.com), además de la comprobación de @Email. */
    static final String EMAIL_OPCIONAL = "^(?:[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,})?$";

    private static final String LETRAS_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final Pattern DNI = Pattern.compile("^\\d{8}[A-Z]$");
    private static final Pattern NIE = Pattern.compile("^[XYZ]\\d{7}[A-Z]$");
    private static final Pattern PASAPORTE = Pattern.compile("^[A-Z0-9]{5,20}$");
    private static final Pattern OTRO = Pattern.compile("^[A-Z0-9]{3,20}$");

    private ValidacionPacientes() {
    }

    /**
     * El número tal y como se guarda: en mayúsculas y sin espacios, puntos, guiones ni barras
     * ("12.345.678-z" → "12345678Z"). Un DNI de 7 cifras se completa con un 0 delante.
     */
    public static String normalizarDocumento(TipoDocumento tipo, String numero) {
        if (numero == null) return null;
        String limpio = numero.replaceAll("[\\s./-]", "").toUpperCase(Locale.ROOT);
        if (limpio.isEmpty()) return null;
        if (tipo == TipoDocumento.DNI && limpio.matches("^\\d{7}[A-Z]$")) return "0" + limpio;
        return limpio;
    }

    /** Qué falla en un número de documento ya normalizado, o null si es correcto. DNI y NIE comprueban su letra. */
    public static String errorDelNumero(TipoDocumento tipo, String numero) {
        return switch (tipo) {
            case DNI -> !DNI.matcher(numero).matches()
                    ? "El DNI son 8 números y una letra, por ejemplo 12345678Z."
                    : letraCorrecta(numero.substring(0, 8), numero.charAt(8)) ? null
                    : "La letra no corresponde a ese número de DNI. Revísalo.";
            case NIE -> !NIE.matcher(numero).matches()
                    ? "El NIE es X, Y o Z, 7 números y una letra, por ejemplo X1234567L."
                    : letraCorrecta("XYZ".indexOf(numero.charAt(0)) + numero.substring(1, 8), numero.charAt(8)) ? null
                    : "La letra no corresponde a ese NIE. Revísalo.";
            case PASAPORTE -> PASAPORTE.matcher(numero).matches() ? null
                    : "El pasaporte debe tener entre 5 y 20 letras o números.";
            case OTRO -> OTRO.matcher(numero).matches() ? null
                    : "El documento debe tener entre 3 y 20 letras o números.";
        };
    }

    private static boolean letraCorrecta(String cifras, char letra) {
        return LETRAS_DNI.charAt(Integer.parseInt(cifras) % 23) == letra;
    }
}
