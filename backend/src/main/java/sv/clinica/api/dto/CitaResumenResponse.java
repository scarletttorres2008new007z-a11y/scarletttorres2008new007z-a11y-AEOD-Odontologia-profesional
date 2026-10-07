package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OrigenCita;
import sv.clinica.api.entity.Paciente;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Cita en la agenda o en un listado",
        requiredProperties = {"id", "fecha", "hora_inicio", "hora_fin", "estado", "origen", "tratamiento_id",
                "tratamiento", "odontologo_id", "odontologo", "nombre", "telefono"})
public record CitaResumenResponse(
        Long id,
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "10:30") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "11:30") LocalTime horaFin,
        EstadoCita estado,
        OrigenCita origen,
        Long tratamientoId,
        String tratamiento,
        Long odontologoId,
        String odontologo,
        @Schema(description = "Ficha del paciente; vacío si la cita (de la web) aún no está vinculada") Long pacienteId,
        @Schema(description = "Nombre del paciente: el de su ficha o, sin ficha, el que escribió en la web") String nombre,
        @Schema(description = "Teléfono de su ficha o, sin ficha, el que dejó en la web") String telefono) {

    public static CitaResumenResponse from(Cita c) {
        Paciente p = c.getPaciente();
        return new CitaResumenResponse(c.getId(), c.getFecha(), c.getHoraInicio(), c.getHoraFin(), c.getEstado(),
                c.getOrigen(), c.getTratamiento().getId(), c.getTratamiento().getNombre(), c.getOdontologo().getId(),
                c.getOdontologo().getNombre(), p == null ? null : p.getId(),
                p == null ? c.getNombre() : p.getNombres() + " " + p.getApellidos(),
                p == null ? c.getTelefono() : p.getTelefono());
    }
}
