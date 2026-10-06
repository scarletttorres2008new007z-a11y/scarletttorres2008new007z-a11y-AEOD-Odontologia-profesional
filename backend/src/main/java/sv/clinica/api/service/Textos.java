package sv.clinica.api.service;

/** Utilidad mínima para normalizar textos opcionales. */
final class Textos {

    private Textos() {
    }

    /** «Tiene 1 cita pendiente o confirmada de hoy en adelante.» / «Tiene 3 citas pendientes o confirmadas…». */
    static String citasPendientes(int total) {
        return total == 1
                ? "Tiene 1 cita pendiente o confirmada de hoy en adelante."
                : "Tiene " + total + " citas pendientes o confirmadas de hoy en adelante.";
    }

    /** Recorta espacios y convierte "" en null. */
    static String opcional(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
