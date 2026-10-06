package sv.clinica.api.service;

import org.springframework.stereotype.Component;
import sv.clinica.api.dto.CitaResumenResponse;
import sv.clinica.api.dto.CitasAfectadasResponse;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.repository.CitaRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * Citas pendientes o confirmadas, de hoy en adelante, a las que afecta un cambio de la configuración de la agenda
 * (un bloqueo, un horario o un turno nuevos). Nunca se cancelan solas: se avisa para que la clínica las mueva o las
 * cancele, y la reserva sigue siendo válida hasta entonces.
 */
@Component
public class CitasAfectadas {

    /** Las citas se dan como mucho a 180 días vista: con un año se miran todas las futuras. */
    private static final int DIAS_VISTA = 366;
    private static final int MAXIMO_EN_LA_LISTA = 50;

    private final CitaRepository citas;
    private final AccesoACitas acceso;
    private final Clock clock;

    public CitasAfectadas(CitaRepository citas, AccesoACitas acceso, Clock clock) {
        this.citas = citas;
        this.acceso = acceso;
        this.clock = clock;
    }

    /** Las que cumplen la condición, por fecha y hora. */
    public List<Cita> buscar(Predicate<Cita> afectada) {
        LocalDate hoy = LocalDate.now(clock);
        return citas.findParaAgenda(hoy, hoy.plusDays(DIAS_VISTA), EstadoCita.MODIFICABLES, null).stream()
                .filter(afectada)
                .toList();
    }

    /** Cuántas son y, si quien hace el cambio puede ver todas las agendas, cuáles (sus datos son de pacientes). */
    public CitasAfectadasResponse respuesta(List<Cita> afectadas) {
        List<CitaResumenResponse> lista = acceso.delUsuarioActual().todas()
                ? afectadas.stream().limit(MAXIMO_EN_LA_LISTA).map(CitaResumenResponse::from).toList()
                : null;
        return new CitasAfectadasResponse(afectadas.size(), lista);
    }
}
