package sv.clinica.landing.dto;

/** Expresiones regulares compartidas por ContactoRequest y CitaRequest. */
final class ValidacionPatrones {

    /** Letras (con tildes y ñ), espacios, apóstrofo, punto y guion. */
    static final String NOMBRE = "^\\p{L}[\\p{L} '.-]*$";

    /** Entre 9 y 15 dígitos; admite + inicial, espacios, paréntesis, puntos y guiones. */
    static final String TELEFONO = "^\\+?(?:[ ().-]*\\d){9,15}[ ().-]*$";

    /** Exige dominio con punto (nombre@dominio.com), además de la comprobación estándar de @Email. */
    static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";

    private ValidacionPatrones() {
    }
}
