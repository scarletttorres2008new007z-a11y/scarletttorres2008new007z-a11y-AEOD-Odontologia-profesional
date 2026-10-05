package sv.clinica.landing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.landing.config.AgendaProperties;
import sv.clinica.landing.dto.DisponibilidadResponse;
import sv.clinica.landing.dto.HorarioDisponibleResponse;
import sv.clinica.landing.entity.Bloqueo;
import sv.clinica.landing.entity.Cita;
import sv.clinica.landing.entity.EstadoCita;
import sv.clinica.landing.entity.Franja;
import sv.clinica.landing.entity.HorarioClinica;
import sv.clinica.landing.entity.HorarioOdontologo;
import sv.clinica.landing.entity.Odontologo;
import sv.clinica.landing.entity.Tratamiento;
import sv.clinica.landing.exception.DatosInvalidosException;
import sv.clinica.landing.repository.BloqueoRepository;
import sv.clinica.landing.repository.CitaRepository;
import sv.clinica.landing.repository.HorarioClinicaRepository;
import sv.clinica.landing.repository.HorarioOdontologoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula los horarios realmente disponibles. Es la única fuente de verdad de la agenda:
 * la landing (y en el futuro el software de clínica y la app) solo muestran lo que devuelve.
 *
 * Para cada día y cada odontólogo que realiza el tratamiento:
 *   turno del odontólogo ∩ horario de la clínica − bloqueos (almuerzo, feriados…) − citas activas
 * y dentro de lo que queda se buscan bloques continuos de la duración del tratamiento.
 */
@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    static final String SIN_DISPONIBILIDAD = "No encontramos disponibilidad para el horario seleccionado.";
    private static final int MAX_OPCIONES_POR_DIA = 3;
    private static final int SEPARACION_OPCIONES_MINUTOS = 60;

    /** Un hueco libre concreto. */
    public record Horario(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Odontologo odontologo) {
    }

    private final HorarioClinicaRepository horariosClinica;
    private final HorarioOdontologoRepository horariosOdontologo;
    private final BloqueoRepository bloqueos;
    private final CitaRepository citas;
    private final TratamientoService tratamientoService;
    private final OdontologoService odontologoService;
    private final AgendaProperties reglas;
    private final Clock clock;

    public DisponibilidadService(HorarioClinicaRepository horariosClinica, HorarioOdontologoRepository horariosOdontologo,
                                 BloqueoRepository bloqueos, CitaRepository citas, TratamientoService tratamientoService,
                                 OdontologoService odontologoService, AgendaProperties reglas, Clock clock) {
        this.horariosClinica = horariosClinica;
        this.horariosOdontologo = horariosOdontologo;
        this.bloqueos = bloqueos;
        this.citas = citas;
        this.tratamientoService = tratamientoService;
        this.odontologoService = odontologoService;
        this.reglas = reglas;
        this.clock = clock;
    }

    /* ─────────────── Consultas para la API ─────────────── */

    /** Horarios de un día. Si no hay ninguno, añade las próximas opciones de los días siguientes. */
    public DisponibilidadResponse consultarDia(Long tratamientoId, LocalDate fecha, Long odontologoId, Franja franja) {
        Tratamiento tratamiento = tratamientoService.buscarActivo(tratamientoId);
        Odontologo odontologo = odontologoOpcional(tratamiento, odontologoId);
        validarFecha(fecha, "fecha");

        List<Horario> horarios = buscar(tratamiento, fecha, odontologo, franja);
        if (!horarios.isEmpty()) {
            return DisponibilidadResponse.de(fecha, tratamiento, horarios, null, null);
        }
        List<Horario> proximas = proximos(tratamiento, fecha.plusDays(1), odontologo, franja, 6);
        if (proximas.isEmpty() && franja != null) {
            // Si con esa franja no hay nada, cualquier hora es mejor que ninguna opción
            proximas = proximos(tratamiento, fecha, odontologo, null, 6);
        }
        return DisponibilidadResponse.de(fecha, tratamiento, horarios, proximas, SIN_DISPONIBILIDAD);
    }

    /** Los huecos más cercanos a partir de una fecha (botón "Buscar el horario más cercano"). */
    public DisponibilidadResponse consultarProximos(Long tratamientoId, LocalDate desde, Long odontologoId, Franja franja,
                                                    int limite) {
        Tratamiento tratamiento = tratamientoService.buscarActivo(tratamientoId);
        Odontologo odontologo = odontologoOpcional(tratamiento, odontologoId);
        LocalDate inicio = desde == null || desde.isBefore(hoy()) ? hoy() : desde;
        List<Horario> horarios = proximos(tratamiento, inicio, odontologo, franja, Math.max(1, Math.min(limite, 20)));
        return DisponibilidadResponse.de(null, tratamiento, horarios, null, horarios.isEmpty() ? SIN_DISPONIBILIDAD : null);
    }

    /* ─────────────── Lógica de agenda (también la usa CitaService) ─────────────── */

    /** Huecos de un día. Sin odontólogo concreto, se ofrece cada hora una sola vez con el odontólogo más libre. */
    public List<Horario> buscar(Tratamiento tratamiento, LocalDate fecha, Odontologo odontologo, Franja franja) {
        Agenda agenda = cargar(fecha, fecha);
        return delDia(agenda, tratamiento, fecha, odontologo, franja, odontologo == null);
    }

    /** ¿Sigue libre ese hueco exacto? Se comprueba otra vez justo antes de guardar una cita. */
    public boolean estaDisponible(Tratamiento tratamiento, Odontologo odontologo, LocalDate fecha, LocalTime horaInicio) {
        if (fecha.isBefore(hoy()) || fecha.isAfter(fechaMaxima())) return false;
        return buscar(tratamiento, fecha, odontologo, null).stream()
                .anyMatch(h -> h.horaInicio().equals(horaInicio));
    }

    /** Próximos huecos a partir de {@code desde}: como mucho 3 por día y separados al menos una hora. */
    public List<Horario> proximos(Tratamiento tratamiento, LocalDate desde, Odontologo odontologo, Franja franja, int limite) {
        LocalDate hasta = desde.plusDays(reglas.diasBusquedaAlternativas());
        if (hasta.isAfter(fechaMaxima())) hasta = fechaMaxima();
        if (desde.isAfter(hasta)) return List.of();

        Agenda agenda = cargar(desde, hasta);
        List<Horario> resultado = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta) && resultado.size() < limite; dia = dia.plusDays(1)) {
            int delDia = 0;
            LocalTime ultima = null;
            for (Horario h : delDia(agenda, tratamiento, dia, odontologo, franja, true)) {
                if (delDia == MAX_OPCIONES_POR_DIA || resultado.size() == limite) break;
                if (ultima != null && h.horaInicio().isBefore(ultima.plusMinutes(SEPARACION_OPCIONES_MINUTOS))) continue;
                resultado.add(h);
                ultima = h.horaInicio();
                delDia++;
            }
        }
        return resultado;
    }

    public LocalDate hoy() {
        return LocalDate.now(clock);
    }

    public LocalDate fechaMaxima() {
        return hoy().plusDays(reglas.diasReservaMaximos());
    }

    /** La fecha debe ser de hoy en adelante y dentro del plazo de reserva. */
    public void validarFecha(LocalDate fecha, String campo) {
        if (fecha.isBefore(hoy())) {
            throw new DatosInvalidosException(campo, "La fecha no puede ser anterior a hoy.");
        }
        if (fecha.isAfter(fechaMaxima())) {
            throw new DatosInvalidosException(campo,
                    "Solo se pueden reservar citas en los próximos " + reglas.diasReservaMaximos() + " días.");
        }
    }

    /* ─────────────── Cálculo ─────────────── */

    private List<Horario> delDia(Agenda agenda, Tratamiento tratamiento, LocalDate fecha, Odontologo filtro,
                                 Franja franja, boolean unaVezPorHora) {
        HorarioClinica clinica = agenda.clinica.get(fecha.getDayOfWeek().getValue());
        if (clinica == null || fecha.isBefore(hoy())) return List.of();

        Tramo abierto = Tramo.de(clinica.getHoraApertura(), clinica.getHoraCierre());
        int duracion = duracionDe(tratamiento);
        int paso = Math.max(5, reglas.intervaloMinutos());
        int primerInicio = fecha.equals(hoy())
                ? Tramo.minutos(LocalTime.now(clock)) + reglas.antelacionMinimaMinutos()
                : 0;
        int inicioTarde = Tramo.minutos(reglas.inicioTarde());

        // hora de inicio → odontólogos libres a esa hora
        Map<Integer, List<Odontologo>> porHora = new TreeMap<>();
        Map<Long, List<Tramo>> turnosDelDia = agenda.turnos.getOrDefault(fecha.getDayOfWeek().getValue(), Map.of());

        for (Map.Entry<Long, List<Tramo>> turno : turnosDelDia.entrySet()) {
            Odontologo odontologo = agenda.odontologos.get(turno.getKey());
            if (!tratamiento.loRealiza(odontologo) || (filtro != null && !filtro.equals(odontologo))) continue;

            List<Tramo> libres = Tramo.restar(Tramo.intersectar(turno.getValue(), abierto),
                    ocupado(agenda, odontologo, fecha));

            for (Tramo libre : libres) {
                int desde = Math.max(libre.inicio(), primerInicio);
                for (int inicio = redondearArriba(desde, paso); inicio + duracion <= libre.fin(); inicio += paso) {
                    if (franja == Franja.MANANA && inicio >= inicioTarde) continue;
                    if (franja == Franja.TARDE && inicio < inicioTarde) continue;
                    porHora.computeIfAbsent(inicio, k -> new ArrayList<>()).add(odontologo);
                }
            }
        }

        Comparator<Odontologo> masLibrePrimero = Comparator
                .comparingInt((Odontologo o) -> agenda.minutosReservados(o, fecha))
                .thenComparingInt(Odontologo::getOrden)
                .thenComparing(Odontologo::getId);

        List<Horario> resultado = new ArrayList<>();
        porHora.forEach((inicio, odontologos) -> {
            odontologos.sort(masLibrePrimero);
            List<Odontologo> elegidos = unaVezPorHora ? odontologos.subList(0, 1) : odontologos;
            for (Odontologo o : elegidos) {
                resultado.add(new Horario(fecha, Tramo.hora(inicio), Tramo.hora(inicio + duracion), o));
            }
        });
        return resultado;
    }

    /** Bloqueos y citas activas de ese odontólogo ese día. Un bloqueo de día completo lo ocupa todo. */
    private static List<Tramo> ocupado(Agenda agenda, Odontologo odontologo, LocalDate fecha) {
        List<Tramo> ocupado = new ArrayList<>();
        for (Bloqueo b : agenda.bloqueos) {
            if (!b.aplicaA(odontologo, fecha)) continue;
            ocupado.add(b.esDiaCompleto() ? new Tramo(0, 24 * 60) : Tramo.de(b.getHoraInicio(), b.getHoraFin()));
        }
        ocupado.addAll(agenda.citas.getOrDefault(new Clave(odontologo.getId(), fecha), List.of()));
        return ocupado;
    }

    public static int duracionDe(Tratamiento tratamiento) {
        Integer minutos = tratamiento.getDuracionMinutos();
        return minutos == null || minutos <= 0 ? 30 : minutos;
    }

    private static int redondearArriba(int minutos, int paso) {
        return ((minutos + paso - 1) / paso) * paso;
    }

    private Odontologo odontologoOpcional(Tratamiento tratamiento, Long odontologoId) {
        if (odontologoId == null) return null;
        Odontologo odontologo = odontologoService.buscarActivo(odontologoId);
        if (!tratamiento.loRealiza(odontologo)) {
            throw new DatosInvalidosException("odontologo_id", "Este odontólogo no realiza el tratamiento seleccionado.");
        }
        return odontologo;
    }

    /* ─────────────── Datos de agenda de un rango de fechas (una sola carga) ─────────────── */

    private record Clave(Long odontologoId, LocalDate fecha) {
    }

    private static final class Agenda {
        Map<Integer, HorarioClinica> clinica;
        Map<Long, Odontologo> odontologos = new HashMap<>();
        /** día de la semana → odontólogo → turnos */
        Map<Integer, Map<Long, List<Tramo>>> turnos = new HashMap<>();
        List<Bloqueo> bloqueos;
        Map<Clave, List<Tramo>> citas = new HashMap<>();

        int minutosReservados(Odontologo o, LocalDate fecha) {
            return citas.getOrDefault(new Clave(o.getId(), fecha), List.of()).stream()
                    .mapToInt(t -> t.fin() - t.inicio()).sum();
        }
    }

    private Agenda cargar(LocalDate desde, LocalDate hasta) {
        Agenda agenda = new Agenda();
        agenda.clinica = horariosClinica.findAll().stream()
                .collect(Collectors.toMap(HorarioClinica::getDiaSemana, Function.identity(), (a, b) -> a));
        for (HorarioOdontologo h : horariosOdontologo.findDeOdontologosActivos()) {
            agenda.odontologos.putIfAbsent(h.getOdontologo().getId(), h.getOdontologo());
            agenda.turnos.computeIfAbsent(h.getDiaSemana(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(h.getOdontologo().getId(), k -> new ArrayList<>())
                    .add(Tramo.de(h.getHoraInicio(), h.getHoraFin()));
        }
        agenda.bloqueos = bloqueos.findActivosEntre(desde, hasta);
        for (Cita c : citas.findEntreFechas(desde, hasta, EstadoCita.OCUPAN_AGENDA)) {
            agenda.citas.computeIfAbsent(new Clave(c.getOdontologo().getId(), c.getFecha()), k -> new ArrayList<>())
                    .add(Tramo.de(c.getHoraInicio(), c.getHoraFin()));
        }
        return agenda;
    }

    /** Momento actual en la zona horaria de la clínica. */
    LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
