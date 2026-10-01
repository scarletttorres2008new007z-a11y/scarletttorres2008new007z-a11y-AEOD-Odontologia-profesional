package sv.clinica.landing.service;

/** Utilidad mínima para normalizar textos opcionales. */
final class Textos {

    private Textos() {
    }

    /** Recorta espacios y convierte "" en null. */
    static String opcional(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
