package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.api.entity.HorarioClinica;

import java.util.List;

public interface HorarioClinicaRepository extends JpaRepository<HorarioClinica, Long> {

    List<HorarioClinica> findAllByOrderByDiaSemanaAsc();
}
