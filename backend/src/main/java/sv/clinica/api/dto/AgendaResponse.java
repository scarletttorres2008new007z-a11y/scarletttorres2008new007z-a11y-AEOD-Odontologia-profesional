package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.TipoBloqueo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Lo que el software pinta en la agenda de unos días: cuándo abre la clínica, el turno de cada odontólogo, los
 * bloqueos y las citas que ocupan hueco. El software solo lo pinta: si un hueco está libre lo decide siempre el backend.
 */
@Schema(requiredProperties = {"desde", "hasta", "dias", "odontologos", "turnos", "bloqueos", "citas",
        "solo_su_agenda"})
public record AgendaResponse(
        LocalDate desde,
        LocalDate hasta,
        List<Dia> dias,
        @Schema(description = "Odontólogos que se muestran, en orden") List<OdontologoDeAgenda> odontologos,
        List<Turno> turnos,
        List<BloqueoDelDia> bloqueos,
        @Schema(description = "Citas que ocupan su hueco (las canceladas y reprogramadas no salen)") List<CitaResumenResponse> citas,
        @Schema(description = "true = el usuario solo puede ver la agenda del odontólogo vinculado a él") boolean soloSuAgenda) {

    @Schema(requiredProperties = {"fecha"})
    public record Dia(
            LocalDate fecha,
            @Schema(type = "string", description = "Vacío = la clínica cierra ese día") @JsonFormat(pattern = "HH:mm") LocalTime apertura,
            @Schema(type = "string") @JsonFormat(pattern = "HH:mm") LocalTime cierre) {
    }

    @Schema(requiredProperties = {"id", "nombre"})
    public record OdontologoDeAgenda(Long id, String nombre) {
    }

    @Schema(requiredProperties = {"odontologo_id", "fecha", "hora_inicio", "hora_fin"})
    public record Turno(
            Long odontologoId,
            LocalDate fecha,
            @Schema(type = "string") @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @Schema(type = "string") @JsonFormat(pattern = "HH:mm") LocalTime horaFin) {
    }

    @Schema(requiredProperties = {"fecha", "tipo"})
    public record BloqueoDelDia(
            @Schema(description = "Vacío = toda la clínica") Long odontologoId,
            LocalDate fecha,
            @Schema(type = "string", description = "Vacías = el día entero") @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @Schema(type = "string") @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
            TipoBloqueo tipo,
            String motivo) {
    }
}
