package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.landing.entity.SolicitudCita;

public interface SolicitudCitaRepository extends JpaRepository<SolicitudCita, Long> {
}
