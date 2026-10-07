package sv.clinica.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.DiaDeApertura;
import sv.clinica.api.dto.HorarioClinicaRequest;
import sv.clinica.api.dto.HorariosGuardadosResponse;
import sv.clinica.api.dto.HorariosResponse;
import sv.clinica.api.dto.TurnoSemanal;
import sv.clinica.api.dto.TurnosDeOdontologo;
import sv.clinica.api.dto.TurnosRequest;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.HorarioClinica;
import sv.clinica.api.entity.HorarioOdontologo;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.HorarioClinicaRepository;
import sv.clinica.api.repository.HorarioOdontologoRepository;
import sv.clinica.api.repository.OdontologoRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Horario de la clínica y turnos de cada odontólogo. Un hueco solo está libre si cae dentro de los dos (y fuera de
 * los bloqueos y las citas), así que un cambio se nota al momento en la web y en el software. Las citas ya dadas que
 * quedan fuera no se cancelan: se devuelven para que la clínica decida qué hacer con ellas.
 */
@Service
public class ConfiguracionHorariosService {

    private static final String CAMPO_CLINICA = "dias";
    private static final String CAMPO_TURNOS = "turnos";

    private final HorarioClinicaRepository horariosClinica;
    private final HorarioOdontologoRepository horariosOdontologo;
    private final OdontologoRepository odontologos;
    private final CitasAfectadas citasAfectadas;
    private final AuditoriaService auditoria;

    public ConfiguracionHorariosService(HorarioClinicaRepository horariosClinica,
                                        HorarioOdontologoRepository horariosOdontologo,
                                        OdontologoRepository odontologos, CitasAfectadas citasAfectadas,
                                        AuditoriaService auditoria) {
        this.horariosClinica = horariosClinica;
        this.horariosOdontologo = horariosOdontologo;
        this.odontologos = odontologos;
        this.citasAfectadas = citasAfectadas;
        this.auditoria = auditoria;
    }

    /** El horario de la clínica y los turnos de cada odontólogo activo. */
    @Transactional(readOnly = true)
    public HorariosResponse obtener() {
        List<DiaDeApertura> clinica = horariosClinica.findAllByOrderByDiaSemanaAsc().stream()
                .map(DiaDeApertura::from).toList();
        List<TurnosDeOdontologo> turnos = odontologos.findByActivoTrueOrderByOrdenAscIdAsc().stream()
                .map(o -> new TurnosDeOdontologo(o.getId(), o.getNombre(),
                        horariosOdontologo.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(o.getId()).stream()
                                .map(TurnoSemanal::from).toList()))
                .toList();
        return new HorariosResponse(clinica, turnos);
    }

    /** Sustituye el horario de toda la semana; los días que no llegan quedan cerrados. */
    @Transactional
    public HorariosGuardadosResponse cambiarClinica(HorarioClinicaRequest request) {
        List<DiaDeApertura> dias = revisarDias(request.dias());
        Map<String, Object> antes = describirClinica(horariosClinica.findAllByOrderByDiaSemanaAsc().stream()
                .map(DiaDeApertura::from).toList());

        // Borrado en bloque primero: la base de datos no admite dos filas para el mismo día
        horariosClinica.deleteAllInBatch();
        horariosClinica.saveAllAndFlush(dias.stream()
                .map(d -> new HorarioClinica(d.diaSemana(), d.horaApertura(), d.horaCierre())).toList());

        registrarCambios(EntidadAuditoria.HORARIO_CLINICA, null, antes, describirClinica(dias));
        Map<Integer, DiaDeApertura> porDia = dias.stream()
                .collect(Collectors.toMap(DiaDeApertura::diaSemana, d -> d));
        List<Cita> afectadas = citasAfectadas.buscar(c -> {
            DiaDeApertura dia = porDia.get(c.getFecha().getDayOfWeek().getValue());
            return dia == null || c.getHoraInicio().isBefore(dia.horaApertura()) || c.getHoraFin().isAfter(dia.horaCierre());
        });
        return new HorariosGuardadosResponse(obtener(), citasAfectadas.respuesta(afectadas));
    }

    /** Sustituye todos los turnos de la semana de un odontólogo; un día sin tramos no trabaja. */
    @Transactional
    public HorariosGuardadosResponse cambiarTurnos(Long odontologoId, TurnosRequest request) {
        Odontologo odontologo = odontologos.findByIdAndActivoTrue(odontologoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un odontólogo activo con id " + odontologoId + "."));
        List<TurnoSemanal> turnos = revisarTurnos(request.turnos());
        Map<String, Object> antes = describirTurnos(
                horariosOdontologo.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(odontologoId).stream()
                        .map(TurnoSemanal::from).toList());

        horariosOdontologo.borrarTurnosDe(odontologoId);
        horariosOdontologo.saveAllAndFlush(turnos.stream()
                .map(t -> new HorarioOdontologo(odontologo, t.diaSemana(), t.horaInicio(), t.horaFin())).toList());

        registrarCambios(EntidadAuditoria.HORARIO_ODONTOLOGO, odontologoId, antes, describirTurnos(turnos));
        Map<Integer, List<TurnoSemanal>> porDia = turnos.stream().collect(Collectors.groupingBy(TurnoSemanal::diaSemana));
        List<Cita> afectadas = citasAfectadas.buscar(c -> c.getOdontologo().getId().equals(odontologoId)
                && porDia.getOrDefault(c.getFecha().getDayOfWeek().getValue(), List.of()).stream()
                .noneMatch(t -> !c.getHoraInicio().isBefore(t.horaInicio()) && !c.getHoraFin().isAfter(t.horaFin())));
        return new HorariosGuardadosResponse(obtener(), citasAfectadas.respuesta(afectadas));
    }

    /* ─────────────── Validación ─────────────── */

    private static List<DiaDeApertura> revisarDias(List<DiaDeApertura> dias) {
        Map<Integer, DiaDeApertura> porDia = new HashMap<>();
        for (DiaDeApertura dia : dias) {
            if (dia == null) throw new DatosInvalidosException(CAMPO_CLINICA, "Falta uno de los días.");
            HorasDeAgenda.comprobarDia(dia.diaSemana(), CAMPO_CLINICA);
            String cuando = "del " + HorasDeAgenda.dia(dia.diaSemana());
            HorasDeAgenda.comprobarTramo(dia.horaApertura(), dia.horaCierre(), CAMPO_CLINICA, cuando);
            if (porDia.put(dia.diaSemana(), dia) != null) {
                throw new DatosInvalidosException(CAMPO_CLINICA,
                        "El " + HorasDeAgenda.dia(dia.diaSemana()) + " aparece dos veces.");
            }
        }
        return porDia.values().stream().sorted(Comparator.comparing(DiaDeApertura::diaSemana)).toList();
    }

    private static List<TurnoSemanal> revisarTurnos(List<TurnoSemanal> turnos) {
        List<TurnoSemanal> ordenados = new ArrayList<>();
        for (TurnoSemanal turno : turnos) {
            if (turno == null) throw new DatosInvalidosException(CAMPO_TURNOS, "Falta uno de los turnos.");
            HorasDeAgenda.comprobarDia(turno.diaSemana(), CAMPO_TURNOS);
            HorasDeAgenda.comprobarTramo(turno.horaInicio(), turno.horaFin(), CAMPO_TURNOS,
                    "del " + HorasDeAgenda.dia(turno.diaSemana()));
            ordenados.add(turno);
        }
        ordenados.sort(Comparator.comparing(TurnoSemanal::diaSemana).thenComparing(TurnoSemanal::horaInicio));
        for (int i = 1; i < ordenados.size(); i++) {
            TurnoSemanal anterior = ordenados.get(i - 1);
            TurnoSemanal actual = ordenados.get(i);
            if (anterior.diaSemana().equals(actual.diaSemana()) && actual.horaInicio().isBefore(anterior.horaFin())) {
                throw new DatosInvalidosException(CAMPO_TURNOS,
                        "Los turnos del " + HorasDeAgenda.dia(actual.diaSemana()) + " se pisan: "
                                + HorasDeAgenda.tramo(anterior.horaInicio(), anterior.horaFin()) + " y "
                                + HorasDeAgenda.tramo(actual.horaInicio(), actual.horaFin()) + ".");
            }
        }
        return ordenados;
    }

    /* ─────────────── Auditoría ─────────────── */

    /** {lunes: "09:00 a 21:00", …, domingo: "Cerrado"} */
    private static Map<String, Object> describirClinica(List<DiaDeApertura> dias) {
        Map<Integer, DiaDeApertura> porDia = dias.stream().collect(Collectors.toMap(DiaDeApertura::diaSemana, d -> d));
        Map<String, Object> texto = new LinkedHashMap<>();
        for (int dia = 1; dia <= 7; dia++) {
            DiaDeApertura d = porDia.get(dia);
            texto.put(HorasDeAgenda.dia(dia), d == null ? "Cerrado" : HorasDeAgenda.tramo(d.horaApertura(), d.horaCierre()));
        }
        return texto;
    }

    /** {lunes: "09:00 a 13:00 y 16:00 a 20:00", …, domingo: "No trabaja"} */
    private static Map<String, Object> describirTurnos(List<TurnoSemanal> turnos) {
        Map<String, Object> texto = new LinkedHashMap<>();
        for (int dia = 1; dia <= 7; dia++) {
            int esteDia = dia;
            List<String> tramos = turnos.stream().filter(t -> t.diaSemana() == esteDia)
                    .sorted(Comparator.comparing(TurnoSemanal::horaInicio))
                    .map(t -> HorasDeAgenda.tramo(t.horaInicio(), t.horaFin())).toList();
            texto.put(HorasDeAgenda.dia(dia), tramos.isEmpty() ? "No trabaja" : String.join(" y ", tramos));
        }
        return texto;
    }

    private void registrarCambios(EntidadAuditoria entidad, Long id, Map<String, Object> antes, Map<String, Object> despues) {
        // Solo los días que cambian, con cómo estaban y cómo quedan
        antes.keySet().removeIf(dia -> Objects.equals(antes.get(dia), despues.get(dia)));
        despues.keySet().retainAll(antes.keySet());
        if (antes.isEmpty()) return;
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.EDITAR, entidad, id, antes, despues);
    }
}
