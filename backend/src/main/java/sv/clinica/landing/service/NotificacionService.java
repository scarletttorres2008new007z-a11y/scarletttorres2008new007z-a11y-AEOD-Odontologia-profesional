package sv.clinica.landing.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import sv.clinica.landing.config.AgendaProperties;
import sv.clinica.landing.entity.Cita;
import sv.clinica.landing.entity.DestinatarioNotificacion;
import sv.clinica.landing.entity.Franja;
import sv.clinica.landing.entity.ListaEspera;
import sv.clinica.landing.entity.Notificacion;
import sv.clinica.landing.entity.TipoNotificacion;
import sv.clinica.landing.event.CitaEvento;
import sv.clinica.landing.repository.CitaRepository;
import sv.clinica.landing.repository.ListaEsperaRepository;
import sv.clinica.landing.repository.NotificacionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Convierte los cambios de la agenda en avisos pendientes (tabla notificaciones).
 * No envía nada: deja la bandeja preparada para cuando exista un canal (correo, WhatsApp, app).
 *
 *   cita reservada     → aviso al odontólogo
 *   cita cancelada     → aviso al odontólogo + aviso a cada persona de la lista de espera a la que encaje el hueco
 *   cita reprogramada  → aviso al odontólogo + lista de espera para el hueco antiguo
 */
@Service
public class NotificacionService {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionRepository notificaciones;
    private final CitaRepository citas;
    private final ListaEsperaRepository listaEspera;
    private final AgendaProperties reglas;
    private final Clock clock;

    public NotificacionService(NotificacionRepository notificaciones, CitaRepository citas,
                               ListaEsperaRepository listaEspera, AgendaProperties reglas, Clock clock) {
        this.notificaciones = notificaciones;
        this.citas = citas;
        this.listaEspera = listaEspera;
        this.reglas = reglas;
        this.clock = clock;
    }

    @EventListener
    public void alCambiarCita(CitaEvento evento) {
        Cita cita = citas.findById(evento.citaId()).orElseThrow();
        switch (evento.tipo()) {
            case RESERVADA -> avisarOdontologo(TipoNotificacion.NUEVA_CITA, cita, "Nueva cita");
            case CANCELADA -> {
                avisarOdontologo(TipoNotificacion.CITA_CANCELADA, cita, "Cita cancelada");
                avisarListaEspera(cita);
            }
            case REPROGRAMADA -> {
                Cita anterior = citas.findById(evento.citaAnteriorId()).orElseThrow();
                avisarOdontologo(TipoNotificacion.CITA_REPROGRAMADA, cita,
                        "Cita reprogramada (antes " + anterior.getInicio().format(FORMATO) + ")");
                avisarListaEspera(anterior);
            }
        }
    }

    private void avisarOdontologo(TipoNotificacion tipo, Cita cita, String titulo) {
        String detalle = titulo + ": " + cita.getTratamiento().getNombre() + ", "
                + cita.getInicio().format(FORMATO) + ", " + cita.getNombre();
        notificaciones.save(new Notificacion(tipo, DestinatarioNotificacion.ODONTOLOGO, cita.getOdontologo(),
                cita, null, detalle, LocalDateTime.now(clock)));
    }

    /** El hueco de {@code liberada} vuelve a estar libre: avisa a quien lo estaba esperando. */
    private void avisarListaEspera(Cita liberada) {
        Franja franjaHueco = liberada.getHoraInicio().isBefore(reglas.inicioTarde()) ? Franja.MANANA : Franja.TARDE;
        for (ListaEspera espera : listaEspera.findCoincidencias(liberada.getTratamiento(), liberada.getOdontologo(),
                liberada.getFecha())) {
            if (espera.getFranja() != null && espera.getFranja() != franjaHueco) continue;
            String detalle = "Hueco liberado: " + liberada.getTratamiento().getNombre() + ", "
                    + liberada.getInicio().format(FORMATO) + ", con " + liberada.getOdontologo().getNombre();
            notificaciones.save(new Notificacion(TipoNotificacion.HUECO_LIBERADO, DestinatarioNotificacion.LISTA_ESPERA,
                    liberada.getOdontologo(), liberada, espera, detalle, LocalDateTime.now(clock)));
        }
    }
}
