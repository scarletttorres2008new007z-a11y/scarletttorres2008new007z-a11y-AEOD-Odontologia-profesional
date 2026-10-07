package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sv.clinica.api.entity.Paciente;
import sv.clinica.api.entity.Sexo;
import sv.clinica.api.entity.TipoDocumento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Schema(description = "Paciente con sus datos personales, de contacto y administrativos. Las fechas están en hora de Madrid.",
        requiredProperties = {"id", "codigo", "nombres", "apellidos", "telefono", "activo", "creado_en", "actualizado_en"})
public record PacienteResponse(
        Long id,
        @Schema(description = "Código del paciente para la clínica, por ejemplo K7M3QX") String codigo,
        String nombres,
        String apellidos,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        LocalDate fechaNacimiento,
        @Schema(description = "Años cumplidos hoy") Integer edad,
        Sexo sexo,
        @Schema(description = "Solo dígitos, con + delante si lleva prefijo internacional") String telefono,
        String email,
        String direccion,
        String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono,
        String observaciones,
        @Schema(description = "false = dado de baja") boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {

    public static PacienteResponse from(Paciente p, LocalDate hoy) {
        Integer edad = p.getFechaNacimiento() == null ? null : Period.between(p.getFechaNacimiento(), hoy).getYears();
        return new PacienteResponse(p.getId(), p.getCodigo(), p.getNombres(), p.getApellidos(), p.getTipoDocumento(),
                p.getNumeroDocumento(), p.getFechaNacimiento(), edad, p.getSexo(), p.getTelefono(), p.getEmail(),
                p.getDireccion(), p.getContactoEmergenciaNombre(), p.getContactoEmergenciaTelefono(),
                p.getObservaciones(), p.isActivo(), p.getCreadoEn(), p.getActualizadoEn());
    }
}
