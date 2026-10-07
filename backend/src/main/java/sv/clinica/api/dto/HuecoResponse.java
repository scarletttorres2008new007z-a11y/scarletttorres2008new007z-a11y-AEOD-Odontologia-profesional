package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.service.DisponibilidadService.Horario;

import java.time.LocalTime;

@Schema(description = "Hueco libre para dar o mover una cita", requiredProperties = {"odontologo_id", "odontologo",
        "hora_inicio", "hora_fin"})
public record HuecoResponse(
        Long odontologoId,
        String odontologo,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "10:30") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "11:30") LocalTime horaFin) {

    public static HuecoResponse from(Horario h) {
        return new HuecoResponse(h.odontologo().getId(), h.odontologo().getNombre(), h.horaInicio(), h.horaFin());
    }
}
