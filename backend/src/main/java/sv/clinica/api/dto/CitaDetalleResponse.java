package sv.clinica.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OrigenCita;
import sv.clinica.api.entity.Paciente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Cita completa para el personal. Las fechas están en hora de Madrid.",
        requiredProperties = {"id", "fecha", "hora_inicio", "hora_fin", "duracion_minutos", "estado", "origen",
                "tratamiento_id", "tratamiento", "odontologo_id", "odontologo", "contacto", "creada_en",
                "actualizada_en", "estados_siguientes", "modificable"})
public record CitaDetalleResponse(
        Long id,
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "10:30") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "11:30") LocalTime horaFin,
        int duracionMinutos,
        EstadoCita estado,
        OrigenCita origen,
        Long tratamientoId,
        String tratamiento,
        Long odontologoId,
        String odontologo,
        @Schema(description = "Ficha del paciente; vacío si la cita (de la web) aún no está vinculada") PacienteDeCita paciente,
        @Schema(description = "Datos de contacto con los que llegó la cita") ContactoDeCita contacto,
        String notasInternas,
        @Schema(description = "Quién la dio desde el software; vacío si llegó por la web") String creadaPor,
        LocalDateTime creadaEn,
        LocalDateTime actualizadaEn,
        LocalDateTime canceladaEn,
        OrigenCita canceladaPor,
        String motivoCancelacion,
        @Schema(description = "Si es una reprogramación, la cita a la que sustituye") Long citaAnteriorId,
        @Schema(description = "Si se reprogramó, la cita nueva") Long citaNuevaId,
        @Schema(description = "Estados a los que se puede pasar ahora con PUT /api/citas/{id}/estado") List<EstadoCita> estadosSiguientes,
        @Schema(description = "true = todavía se puede cancelar o reprogramar") boolean modificable) {

    @Schema(requiredProperties = {"id", "codigo", "nombre", "telefono"})
    public record PacienteDeCita(Long id, String codigo, String nombre, String telefono, String email) {
    }

    @Schema(requiredProperties = {"nombre", "telefono"})
    public record ContactoDeCita(String nombre, String telefono, String email,
                                 @Schema(description = "Lo que escribió el paciente al reservar en la web") String mensaje) {
    }

    public static CitaDetalleResponse from(Cita c, Long citaNuevaId, List<EstadoCita> estadosSiguientes) {
        Paciente p = c.getPaciente();
        PacienteDeCita paciente = p == null ? null : new PacienteDeCita(p.getId(), p.getCodigo(),
                p.getNombres() + " " + p.getApellidos(), p.getTelefono(), p.getEmail());
        return new CitaDetalleResponse(c.getId(), c.getFecha(), c.getHoraInicio(), c.getHoraFin(),
                c.getDuracionMinutos(), c.getEstado(), c.getOrigen(), c.getTratamiento().getId(),
                c.getTratamiento().getNombre(), c.getOdontologo().getId(), c.getOdontologo().getNombre(), paciente,
                new ContactoDeCita(c.getNombre(), c.getTelefono(), c.getEmail(), c.getMensaje()), c.getNotasInternas(),
                c.getCreadaPor() == null ? null : c.getCreadaPor().getNombre(), c.getCreadaEn(), c.getActualizadaEn(),
                c.getCanceladaEn(), c.getCanceladaPor(), c.getMotivoCancelacion(),
                c.getCitaAnterior() == null ? null : c.getCitaAnterior().getId(), citaNuevaId, estadosSiguientes,
                c.getEstado().esModificable());
    }
}
