package sv.clinica.api.service;

import sv.clinica.api.exception.DatosInvalidosException;

import java.time.LocalTime;

/** Reglas y textos comunes de las horas que se configuran en la agenda (horarios, turnos y bloqueos). */
final class HorasDeAgenda {

    static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private static final int PASO_MINUTOS = 15;

    private HorasDeAgenda() {
    }

    /** «lunes» para el 1 … «domingo» para el 7. */
    static String dia(int diaSemana) {
        return DIAS[diaSemana - 1];
    }

    /** «09:00 a 13:00». */
    static String tramo(LocalTime inicio, LocalTime fin) {
        return inicio + " a " + fin;
    }

    /**
     * La agenda trabaja en cuartos de hora (como la tabla de ocupación que impide la doble reserva): las horas que se
     * configuran tienen que ser en punto, y cuarto, y media o menos cuarto.
     */
    static void comprobarCuartoDeHora(LocalTime hora, String campo) {
        if (hora.getSecond() != 0 || hora.getNano() != 0 || hora.getMinute() % PASO_MINUTOS != 0) {
            throw new DatosInvalidosException(campo, "Usa horas en punto o en cuartos de hora (09:00, 09:15, 09:30…).");
        }
    }

    /** Comprueba un tramo completo: las dos horas, en cuartos de hora y la de fin después de la de inicio. */
    static void comprobarTramo(LocalTime inicio, LocalTime fin, String campo, String cuando) {
        if (inicio == null || fin == null) {
            throw new DatosInvalidosException(campo, "Indica la hora de inicio y la de fin " + cuando + ".");
        }
        comprobarCuartoDeHora(inicio, campo);
        comprobarCuartoDeHora(fin, campo);
        if (!fin.isAfter(inicio)) {
            throw new DatosInvalidosException(campo, "La hora de fin tiene que ser posterior a la de inicio " + cuando + ".");
        }
    }

    static void comprobarDia(Integer diaSemana, String campo) {
        if (diaSemana == null || diaSemana < 1 || diaSemana > 7) {
            throw new DatosInvalidosException(campo, "El día de la semana no es válido.");
        }
    }
}
