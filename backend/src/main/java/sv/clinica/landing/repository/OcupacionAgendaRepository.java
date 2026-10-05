package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.landing.entity.Cita;
import sv.clinica.landing.entity.OcupacionAgenda;

public interface OcupacionAgendaRepository extends JpaRepository<OcupacionAgenda, Long> {

    /** Libera los tramos de una cita (la cita se conserva). */
    @Modifying(flushAutomatically = true)
    @Query("delete from OcupacionAgenda o where o.cita = :cita")
    int liberar(@Param("cita") Cita cita);
}
