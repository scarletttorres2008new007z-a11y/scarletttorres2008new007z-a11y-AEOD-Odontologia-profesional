package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @param clinica     días en que abre la clínica; el que no aparece está cerrado
 * @param odontologos turnos de cada odontólogo activo
 */
@Schema(requiredProperties = {"clinica", "odontologos"})
public record HorariosResponse(List<DiaDeApertura> clinica, List<TurnosDeOdontologo> odontologos) {
}
