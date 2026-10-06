package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.Bloqueo;

import java.time.LocalDate;
import java.util.List;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    /** Bloqueos activos que pueden afectar a algún día entre desde y hasta. */
    @Query("""
            select b from Bloqueo b left join fetch b.odontologo
            where b.activo = true
              and (b.fechaInicio is null or b.fechaInicio <= :hasta)
              and (b.fechaFin is null or b.fechaFin >= :desde)
            """)
    List<Bloqueo> findActivosEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Bloqueos activos que aún no han terminado: los que no tienen fin y los que acaban hoy o después. */
    @Query("""
            select b from Bloqueo b left join fetch b.odontologo
            where b.activo = true and (b.fechaFin is null or b.fechaFin >= :hoy)
            """)
    List<Bloqueo> findVigentes(@Param("hoy") LocalDate hoy);
}
