package sv.clinica.api.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import sv.clinica.api.entity.Paciente;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Búsqueda de pacientes. El texto se parte en palabras y cada palabra tiene que aparecer en el nombre, los apellidos,
 * el documento, el teléfono, el correo o el código, así que "garcia jose" encuentra a "José García".
 * Mayúsculas y tildes dan igual (lo resuelve la intercalación utf8mb4_unicode_ci de la tabla).
 * Los valores van siempre como parámetros de la consulta, nunca pegados al SQL.
 */
final class BusquedaDePacientes {

    /** Se tienen en cuenta como mucho estas palabras. */
    private static final int PALABRAS_MAXIMAS = 6;

    private BusquedaDePacientes() {
    }

    static Specification<Paciente> de(String texto, Boolean activo) {
        List<String> palabras = palabras(texto);
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (activo != null) condiciones.add(cb.equal(raiz.get("activo"), activo));
            for (String palabra : palabras) {
                String patron = patron(palabra);
                List<Predicate> alguna = new ArrayList<>(List.of(
                        cb.like(raiz.get("nombres"), patron, '!'),
                        cb.like(raiz.get("apellidos"), patron, '!'),
                        cb.like(raiz.get("email"), patron, '!'),
                        cb.like(raiz.get("codigo"), patron, '!')));
                // El documento y el teléfono se guardan sin espacios ni guiones: se buscan igual
                String documento = palabra.replaceAll("[^\\p{L}\\p{N}]", "");
                if (!documento.isEmpty()) alguna.add(cb.like(raiz.get("numeroDocumento"), patron(documento), '!'));
                String telefono = palabra.replaceAll("[^+\\d]", "");
                if (!telefono.isEmpty()) alguna.add(cb.like(raiz.get("telefono"), patron(telefono), '!'));
                condiciones.add(cb.or(alguna.toArray(Predicate[]::new)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static List<String> palabras(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.trim().toLowerCase(Locale.ROOT).split("\\s+")).limit(PALABRAS_MAXIMAS).toList();
    }

    /** "50%" → "%50!%%": los comodines de LIKE escritos por la persona se buscan tal cual. */
    private static String patron(String palabra) {
        return "%" + palabra.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
