package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Bloqueo;
import sv.clinica.api.entity.TipoBloqueo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tiempo bloqueado en la agenda. Los campos vacíos no limitan: sin odontólogo es toda la clínica, sin fechas es
 * siempre, sin día de la semana es cada día y sin horas es el día entero.
 */
@Schema(requiredProperties = {"id", "tipo"})
public record BloqueoResponse(
        Long id,
        TipoBloqueo tipo,
        String motivo,
        Long odontologoId,
        String odontologo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        @Schema(description = "1 = lunes … 7 = domingo") Integer diaSemana,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "12:00") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "13:00") LocalTime horaFin) {

    public static BloqueoResponse from(Bloqueo b) {
        return new BloqueoResponse(b.getId(), b.getTipo(), b.getMotivo(),
                b.getOdontologo() == null ? null : b.getOdontologo().getId(),
                b.getOdontologo() == null ? null : b.getOdontologo().getNombre(),
                b.getFechaInicio(), b.getFechaFin(), b.getDiaSemana(), b.getHoraInicio(), b.getHoraFin());
    }
}
