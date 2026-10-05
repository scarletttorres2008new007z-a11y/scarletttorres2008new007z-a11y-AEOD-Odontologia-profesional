package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.landing.entity.HorarioClinica;

public interface HorarioClinicaRepository extends JpaRepository<HorarioClinica, Long> {
}
