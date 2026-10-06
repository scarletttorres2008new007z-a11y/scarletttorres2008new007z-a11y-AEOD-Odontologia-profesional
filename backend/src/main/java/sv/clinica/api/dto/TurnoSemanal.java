package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.HorarioOdontologo;

import java.time.LocalTime;

/** Un tramo de trabajo de un odontólogo un día de la semana (1 = lunes … 7 = domingo). */
@Schema(requiredProperties = {"dia_semana", "hora_inicio", "hora_fin"})
public record TurnoSemanal(
        @Schema(minimum = "1", maximum = "7", example = "1") Integer diaSemana,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "09:00") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "13:00") LocalTime horaFin) {

    public static TurnoSemanal from(HorarioOdontologo h) {
        return new TurnoSemanal(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin());
    }
}
