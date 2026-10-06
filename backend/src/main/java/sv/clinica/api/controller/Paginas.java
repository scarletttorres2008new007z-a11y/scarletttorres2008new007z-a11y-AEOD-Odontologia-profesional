package sv.clinica.api.controller;

/** Límites de la paginación: la página empieza en 0 y como máximo se devuelven 100 resultados por página. */
final class Paginas {

    static final int TAMANO_MAXIMO = 100;

    private Paginas() {
    }

    static int pagina(int pagina) {
        return Math.max(0, pagina);
    }

    static int tamano(int tamano) {
        return Math.min(Math.max(1, tamano), TAMANO_MAXIMO);
    }
}
