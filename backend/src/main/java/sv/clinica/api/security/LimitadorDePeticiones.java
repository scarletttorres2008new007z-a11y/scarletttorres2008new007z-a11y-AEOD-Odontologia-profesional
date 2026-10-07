package sv.clinica.api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;
import sv.clinica.api.config.LimitesProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuenta las peticiones de cada IP por tipo (login, reservas, contacto) con Bucket4j.
 * Los contadores viven en memoria; si hubiera varios servidores, cada uno llevaría los suyos.
 */
@Component
public class LimitadorDePeticiones {

    /** Contadores guardados como máximo; se descartan los que llevan más tiempo sin usarse. */
    private static final int MAXIMO_CONTADORES = 10_000;

    public enum Tipo { LOGIN, RESERVA, CONTACTO }

    private final LimitesProperties limites;
    private final Map<String, Bucket> contadores = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> masAntiguo) {
                    return size() > MAXIMO_CONTADORES;
                }
            });

    public LimitadorDePeticiones(LimitesProperties limites) {
        this.limites = limites;
    }

    /** @return 0 si la petición puede seguir; si no, los segundos que hay que esperar */
    public long consumir(Tipo tipo, String ip) {
        ConsumptionProbe intento = contadores.computeIfAbsent(tipo + "|" + ip, clave -> nuevoContador(tipo))
                .tryConsumeAndReturnRemaining(1);
        if (intento.isConsumed()) return 0;
        return Math.max(1, Duration.ofNanos(intento.getNanosToWaitForRefill()).toSeconds() + 1);
    }

    /** Pone todos los contadores a cero (pruebas). */
    public void vaciar() {
        contadores.clear();
    }

    private Bucket nuevoContador(Tipo tipo) {
        Bandwidth limite = switch (tipo) {
            case LOGIN -> porPeriodo(limites.loginPorMinuto(), Duration.ofMinutes(1));
            case RESERVA -> porPeriodo(limites.reservasPorHora(), Duration.ofHours(1));
            case CONTACTO -> porPeriodo(limites.contactosPorHora(), Duration.ofHours(1));
        };
        return Bucket.builder().addLimit(limite).build();
    }

    private static Bandwidth porPeriodo(int peticiones, Duration periodo) {
        return Bandwidth.builder().capacity(peticiones).refillGreedy(peticiones, periodo).build();
    }
}
