package sv.clinica.api.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.AgendaResponse;
import sv.clinica.api.dto.AgendaResponse.BloqueoDelDia;
import sv.clinica.api.dto.AgendaResponse.Dia;
import sv.clinica.api.dto.AgendaResponse.OdontologoDeAgenda;
import sv.clinica.api.dto.AgendaResponse.Turno;
import sv.clinica.api.dto.CitaResumenResponse;
import sv.clinica.api.dto.HuecoResponse;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.entity.Bloqueo;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.HorarioClinica;
import sv.clinica.api.entity.HorarioOdontologo;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.repository.BloqueoRepository;
import sv.clinica.api.repository.CitaRepository;
import sv.clinica.api.repository.HorarioClinicaRepository;
import sv.clinica.api.repository.HorarioOdontologoRepository;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.repository.TratamientoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lo que el software consulta de la agenda: la vista de uno o varios días, los listados de citas y los huecos libres
 * para dar o mover una cita. Cada respuesta respeta el alcance de quien pregunta (ver {@link AccesoACitas}).
 */
@Service
@Transactional(readOnly = true)
public class AgendaService {

    static final int DIAS_MAXIMOS = 7;

    private final CitaRepository citas;
    private final OdontologoRepository odontologos;
    private final TratamientoRepository tratamientos;
    private final HorarioClinicaRepository horariosClinica;
    private final HorarioOdontologoRepository horariosOdontologo;
    private final BloqueoRepository bloqueos;
    private final DisponibilidadService disponibilidad;
    private final AccesoACitas acceso;

    public AgendaService(CitaRepository citas, OdontologoRepository odontologos, TratamientoRepository tratamientos,
                         HorarioClinicaRepository horariosClinica, HorarioOdontologoRepository horariosOdontologo,
                         BloqueoRepository bloqueos, DisponibilidadService disponibilidad, AccesoACitas acceso) {
        this.citas = citas;
        this.odontologos = odontologos;
        this.tratamientos = tratamientos;
        this.horariosClinica = horariosClinica;
        this.horariosOdontologo = horariosOdontologo;
        this.bloqueos = bloqueos;
        this.disponibilidad = disponibilidad;
        this.acceso = acceso;
    }

    /** La agenda de {@code dias} días a partir de {@code desde}, de un odontólogo o de todos los que puede ver. */
    public AgendaResponse agenda(LocalDate desde, int dias, Long odontologoId) {
        if (dias < 1 || dias > DIAS_MAXIMOS) {
            throw new DatosInvalidosException("dias", "La agenda se consulta de 1 a " + DIAS_MAXIMOS + " días.");
        }
        LocalDate hasta = desde.plusDays(dias - 1L);
        AccesoACitas.Alcance alcance = acceso.delUsuarioActual();
        Long filtro = alcance.odontologoParaFiltrar(odontologoId);

        List<Cita> ocupadas = citas.findParaAgenda(desde, hasta, EstadoCita.OCUPAN_AGENDA, filtro);

        // Columnas: los odontólogos activos y, si tienen citas en esos días, también los que ya no lo están
        Map<Long, Odontologo> visibles = new LinkedHashMap<>();
        odontologos.findByActivoTrueOrderByOrdenAscIdAsc().stream()
                .filter(o -> filtro == null || filtro.equals(o.getId()))
                .forEach(o -> visibles.put(o.getId(), o));
        ocupadas.forEach(c -> visibles.putIfAbsent(c.getOdontologo().getId(), c.getOdontologo()));

        Map<Integer, HorarioClinica> clinica = horariosClinica.findAll().stream()
                .collect(Collectors.toMap(HorarioClinica::getDiaSemana, Function.identity(), (a, b) -> a));
        List<HorarioOdontologo> horarios = horariosOdontologo.findDeOdontologosActivos();
        List<Bloqueo> vigentes = bloqueos.findActivosEntre(desde, hasta);

        List<Dia> listaDias = new ArrayList<>();
        List<Turno> turnos = new ArrayList<>();
        List<BloqueoDelDia> bloqueosDelDia = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            int diaSemana = fecha.getDayOfWeek().getValue();
            HorarioClinica abierto = clinica.get(diaSemana);
            listaDias.add(new Dia(fecha, abierto == null ? null : abierto.getHoraApertura(),
                    abierto == null ? null : abierto.getHoraCierre()));
            for (HorarioOdontologo h : horarios) {
                if (h.getDiaSemana() == diaSemana && visibles.containsKey(h.getOdontologo().getId())) {
                    turnos.add(new Turno(h.getOdontologo().getId(), fecha, h.getHoraInicio(), h.getHoraFin()));
                }
            }
            for (Bloqueo b : vigentes) {
                boolean deTodaLaClinica = b.getOdontologo() == null;
                if (b.aplicaEl(fecha) && (deTodaLaClinica || visibles.containsKey(b.getOdontologo().getId()))) {
                    bloqueosDelDia.add(new BloqueoDelDia(deTodaLaClinica ? null : b.getOdontologo().getId(), fecha,
                            b.esDiaCompleto() ? null : b.getHoraInicio(), b.esDiaCompleto() ? null : b.getHoraFin(),
                            b.getTipo(), b.getMotivo()));
                }
            }
        }
        turnos.sort(Comparator.comparing(Turno::fecha).thenComparing(Turno::horaInicio));

        return new AgendaResponse(desde, hasta, listaDias,
                visibles.values().stream().map(o -> new OdontologoDeAgenda(o.getId(), o.getNombre())).toList(),
                turnos, bloqueosDelDia, ocupadas.stream().map(CitaResumenResponse::from).toList(), !alcance.todas());
    }

    /** Listado de citas con filtros. «recientes» = de la más nueva a la más antigua (historial de un paciente). */
    public PaginaResponse<CitaResumenResponse> buscar(BusquedaDeCitas busqueda, boolean recientesPrimero,
                                                      int pagina, int tamano) {
        if (busqueda.desde() != null && busqueda.hasta() != null && busqueda.hasta().isBefore(busqueda.desde())) {
            throw new DatosInvalidosException("hasta", "La fecha final no puede ser anterior a la inicial.");
        }
        Long filtro = acceso.delUsuarioActual().odontologoParaFiltrar(busqueda.odontologoId());
        Sort.Direction direccion = recientesPrimero ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest orden = PageRequest.of(pagina, tamano, Sort.by(direccion, "fecha", "horaInicio", "id"));
        return PaginaResponse.de(citas.findAll(busqueda.conOdontologo(filtro).comoFiltro(), orden),
                CitaResumenResponse::from);
    }

    /**
     * Huecos libres de un día para dar o mover una cita: los calcula el mismo servicio que la landing, pero sin la
     * antelación mínima de la web y con cada odontólogo por separado. Al reprogramar, la propia cita no cuenta.
     */
    public List<HuecoResponse> huecos(Long tratamientoId, LocalDate fecha, Long odontologoId, Long excluirCitaId) {
        Tratamiento tratamiento = tratamientos.findByIdAndActivoTrue(tratamientoId)
                .orElseThrow(() -> new DatosInvalidosException("tratamiento_id", "El tratamiento seleccionado no existe."));
        Long filtro = acceso.delUsuarioActual().odontologoParaFiltrar(odontologoId);
        Odontologo odontologo = null;
        if (filtro != null) {
            odontologo = odontologos.findByIdAndActivoTrue(filtro).orElse(null);
            if (odontologo == null) {
                if (filtro.equals(odontologoId)) {
                    throw new DatosInvalidosException("odontologo_id", "El odontólogo seleccionado no existe.");
                }
                return List.of();
            }
            if (!tratamiento.loRealiza(odontologo)) {
                throw new DatosInvalidosException("odontologo_id", "Este odontólogo no realiza el tratamiento seleccionado.");
            }
        }
        disponibilidad.validarFecha(fecha, "fecha");
        return disponibilidad.buscarParaLaClinica(tratamiento, fecha, odontologo, excluirCitaId).stream()
                .map(HuecoResponse::from).toList();
    }
}
