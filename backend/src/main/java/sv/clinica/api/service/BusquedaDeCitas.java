package sv.clinica.api.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;
import sv.clinica.api.entity.OrigenCita;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros del listado de citas. Todos son opcionales; las fechas incluyen ambos días.
 *
 * @param sinFicha true = solo las que aún no están vinculadas a la ficha de un paciente (las de la web)
 */
public record BusquedaDeCitas(LocalDate desde, LocalDate hasta, Long odontologoId, EstadoCita estado,
                              OrigenCita origen, Long pacienteId, Boolean sinFicha) {

    Specification<Cita> comoFiltro() {
        return (cita, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (desde != null) condiciones.add(cb.greaterThanOrEqualTo(cita.get("fecha"), desde));
            if (hasta != null) condiciones.add(cb.lessThanOrEqualTo(cita.get("fecha"), hasta));
            if (odontologoId != null) condiciones.add(cb.equal(cita.get("odontologo").get("id"), odontologoId));
            if (estado != null) condiciones.add(cb.equal(cita.get("estado"), estado));
            if (origen != null) condiciones.add(cb.equal(cita.get("origen"), origen));
            if (pacienteId != null) condiciones.add(cb.equal(cita.get("paciente").get("id"), pacienteId));
            if (Boolean.TRUE.equals(sinFicha)) condiciones.add(cb.isNull(cita.get("paciente")));
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    BusquedaDeCitas conOdontologo(Long odontologo) {
        return new BusquedaDeCitas(desde, hasta, odontologo, estado, origen, pacienteId, sinFicha);
    }
}
