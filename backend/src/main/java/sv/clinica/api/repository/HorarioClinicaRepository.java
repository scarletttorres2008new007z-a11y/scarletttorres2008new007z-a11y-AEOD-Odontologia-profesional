package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.api.entity.HorarioClinica;

public interface HorarioClinicaRepository extends JpaRepository<HorarioClinica, Long> {
}
