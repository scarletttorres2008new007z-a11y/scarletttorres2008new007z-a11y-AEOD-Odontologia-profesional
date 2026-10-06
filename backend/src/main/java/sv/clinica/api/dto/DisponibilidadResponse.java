package sv.clinica.api.dto;

import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.service.DisponibilidadService;
import sv.clinica.api.service.DisponibilidadService.Horario;

import java.time.LocalDate;
import java.util.List;

/**
 * Horarios disponibles calculados por el backend. La landing solo los presenta.
 *
 * @param horarios          huecos del día pedido (o los más cercanos)
 * @param proximasOpciones  solo si ese día no hay huecos: alternativas de los días siguientes
 * @param mensaje           texto para el paciente cuando no hay huecos
 */
public record DisponibilidadResponse(
        LocalDate fecha,
        TratamientoResumen tratamiento,
        List<HorarioDisponibleResponse> horarios,
        List<HorarioDisponibleResponse> proximasOpciones,
        String mensaje) {

    public record TratamientoResumen(Long id, String nombre, int duracionMinutos) {
    }

    public static DisponibilidadResponse de(LocalDate fecha, Tratamiento tratamiento, List<Horario> horarios,
                                            List<Horario> proximas, String mensaje) {
        return new DisponibilidadResponse(
                fecha,
                new TratamientoResumen(tratamiento.getId(), tratamiento.getNombre(), DisponibilidadService.duracionDe(tratamiento)),
                horarios.stream().map(HorarioDisponibleResponse::from).toList(),
                proximas == null ? null : proximas.stream().map(HorarioDisponibleResponse::from).toList(),
                mensaje);
    }
}
