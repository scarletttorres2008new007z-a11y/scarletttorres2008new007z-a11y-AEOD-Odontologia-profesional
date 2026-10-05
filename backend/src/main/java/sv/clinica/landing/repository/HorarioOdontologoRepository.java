package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.clinica.landing.entity.HorarioOdontologo;

import java.util.List;

public interface HorarioOdontologoRepository extends JpaRepository<HorarioOdontologo, Long> {

    @Query("select h from HorarioOdontologo h join fetch h.odontologo o where o.activo = true")
    List<HorarioOdontologo> findDeOdontologosActivos();
}
