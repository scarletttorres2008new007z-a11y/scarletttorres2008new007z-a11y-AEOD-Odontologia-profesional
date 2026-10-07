package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.HorarioClinica;

import java.time.LocalTime;

/** Un día en que abre la clínica (1 = lunes … 7 = domingo). Un día que no aparece está cerrado. */
@Schema(requiredProperties = {"dia_semana", "hora_apertura", "hora_cierre"})
public record DiaDeApertura(
        @Schema(minimum = "1", maximum = "7", example = "1") Integer diaSemana,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "09:00") LocalTime horaApertura,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "21:00") LocalTime horaCierre) {

    public static DiaDeApertura from(HorarioClinica h) {
        return new DiaDeApertura(h.getDiaSemana(), h.getHoraApertura(), h.getHoraCierre());
    }
}
