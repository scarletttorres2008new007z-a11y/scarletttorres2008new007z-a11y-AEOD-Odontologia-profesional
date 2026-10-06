package sv.clinica.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    Optional<Cita> findByCodigo(String codigo);

    /** Citas que ocupan agenda entre dos fechas (ambas incluidas). */
    @Query("""
            select c from Cita c join fetch c.odontologo
            where c.fecha between :desde and :hasta and c.estado in :estados
            """)
    List<Cita> findEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                               @Param("estados") Collection<EstadoCita> estados);

    /** Citas para pintar la agenda, con su tratamiento, odontólogo y paciente ya cargados. Sin odontólogo = todos. */
    @Query("""
            select c from Cita c join fetch c.tratamiento join fetch c.odontologo left join fetch c.paciente
            where c.fecha between :desde and :hasta and c.estado in :estados
              and (:odontologoId is null or c.odontologo.id = :odontologoId)
            order by c.fecha, c.horaInicio, c.id
            """)
    List<Cita> findParaAgenda(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                              @Param("estados") Collection<EstadoCita> estados,
                              @Param("odontologoId") Long odontologoId);

    /** Listados con filtros: trae a la vez el tratamiento, el odontólogo y el paciente de cada cita. */
    @Override
    @EntityGraph(attributePaths = {"tratamiento", "odontologo", "paciente"})
    Page<Cita> findAll(Specification<Cita> filtro, Pageable pagina);

    /** La cita que sustituyó a esta al reprogramarla, si la hay. */
    @Query("select c.id from Cita c where c.citaAnterior.id = :id")
    Optional<Long> findIdDeLaNueva(@Param("id") Long id);
}
