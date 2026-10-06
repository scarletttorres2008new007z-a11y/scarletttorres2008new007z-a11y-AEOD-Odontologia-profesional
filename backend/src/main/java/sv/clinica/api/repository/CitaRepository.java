package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.EstadoCita;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByCodigo(String codigo);

    /** Citas que ocupan agenda entre dos fechas (ambas incluidas). */
    @Query("""
            select c from Cita c join fetch c.odontologo
            where c.fecha between :desde and :hasta and c.estado in :estados
            """)
    List<Cita> findEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                               @Param("estados") Collection<EstadoCita> estados);
}
