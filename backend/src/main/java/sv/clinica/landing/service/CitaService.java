package sv.clinica.landing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.landing.dto.CitaRequest;
import sv.clinica.landing.entity.SolicitudCita;
import sv.clinica.landing.entity.Tratamiento;
import sv.clinica.landing.exception.DatosInvalidosException;
import sv.clinica.landing.exception.RecursoNoEncontradoException;
import sv.clinica.landing.repository.SolicitudCitaRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Registra solicitudes de cita. Una solicitud NO confirma la cita:
 * se guarda como PENDIENTE y la clínica contacta a la persona.
 */
@Service
public class CitaService {

    static final LocalTime HORA_MINIMA = LocalTime.of(7, 0);
    static final LocalTime HORA_MAXIMA = LocalTime.of(20, 0);

    private final SolicitudCitaRepository repository;
    private final TratamientoService tratamientoService;
    private final Clock clock;

    public CitaService(SolicitudCitaRepository repository, TratamientoService tratamientoService, Clock clock) {
        this.repository = repository;
        this.tratamientoService = tratamientoService;
        this.clock = clock;
    }

    @Transactional
    public void registrar(CitaRequest request) {
        Tratamiento tratamiento = buscarTratamiento(request.tratamientoId());

        if (request.fechaPreferida().isBefore(LocalDate.now(clock))) {
            throw new DatosInvalidosException("fecha_preferida", "La fecha preferida no puede ser anterior a hoy.");
        }
        LocalTime hora = request.horaPreferida();
        if (hora != null && (hora.isBefore(HORA_MINIMA) || hora.isAfter(HORA_MAXIMA))) {
            throw new DatosInvalidosException("hora_preferida", "La hora preferida debe estar entre las 07:00 y las 20:00.");
        }

        repository.save(new SolicitudCita(
                request.nombre().trim(),
                request.telefono().trim(),
                request.email().trim(),
                tratamiento,
                request.fechaPreferida(),
                hora,
                Textos.opcional(request.mensaje()),
                LocalDateTime.now(clock)));
    }

    /** Un tratamiento inexistente en una solicitud es un dato inválido (400), no una ruta inexistente. */
    private Tratamiento buscarTratamiento(Long id) {
        try {
            return tratamientoService.buscarActivo(id);
        } catch (RecursoNoEncontradoException e) {
            throw new DatosInvalidosException("tratamiento_id", "El tratamiento seleccionado no existe.");
        }
    }
}
