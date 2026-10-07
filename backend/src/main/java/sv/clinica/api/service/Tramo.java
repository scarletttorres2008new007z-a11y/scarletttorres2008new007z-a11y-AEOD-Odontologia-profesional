package sv.clinica.api.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tramo horario [inicio, fin) en minutos desde las 00:00. Base del cálculo de disponibilidad:
 * los turnos se cruzan con el horario de la clínica y se les restan bloqueos y citas.
 */
record Tramo(int inicio, int fin) {

    static Tramo de(LocalTime inicio, LocalTime fin) {
        return new Tramo(minutos(inicio), fin.equals(LocalTime.MIDNIGHT) ? 24 * 60 : minutos(fin));
    }

    static int minutos(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }

    static LocalTime hora(int minutos) {
        return LocalTime.of(minutos / 60, minutos % 60);
    }

    boolean vacio() {
        return fin <= inicio;
    }

    /** Partes de cada tramo de {@code base} que caen dentro de {@code limite}. */
    static List<Tramo> intersectar(List<Tramo> base, Tramo limite) {
        List<Tramo> resultado = new ArrayList<>();
        for (Tramo t : base) {
            Tramo comun = new Tramo(Math.max(t.inicio, limite.inicio), Math.min(t.fin, limite.fin));
            if (!comun.vacio()) resultado.add(comun);
        }
        return unir(resultado);
    }

    /** {@code base} menos todos los tramos de {@code quitar}. */
    static List<Tramo> restar(List<Tramo> base, List<Tramo> quitar) {
        List<Tramo> actual = new ArrayList<>(base);
        for (Tramo q : quitar) {
            List<Tramo> siguiente = new ArrayList<>();
            for (Tramo t : actual) {
                if (q.fin <= t.inicio || q.inicio >= t.fin) {
                    siguiente.add(t);
                    continue;
                }
                if (q.inicio > t.inicio) siguiente.add(new Tramo(t.inicio, q.inicio));
                if (q.fin < t.fin) siguiente.add(new Tramo(q.fin, t.fin));
            }
            actual = siguiente;
        }
        return actual;
    }

    /** Ordena y fusiona tramos que se tocan o se solapan. */
    static List<Tramo> unir(List<Tramo> tramos) {
        List<Tramo> ordenados = new ArrayList<>(tramos);
        ordenados.sort(Comparator.comparingInt(Tramo::inicio));
        List<Tramo> resultado = new ArrayList<>();
        for (Tramo t : ordenados) {
            if (!resultado.isEmpty() && t.inicio <= resultado.get(resultado.size() - 1).fin) {
                Tramo ultimo = resultado.remove(resultado.size() - 1);
                resultado.add(new Tramo(ultimo.inicio, Math.max(ultimo.fin, t.fin)));
            } else {
                resultado.add(t);
            }
        }
        return resultado;
    }
}
