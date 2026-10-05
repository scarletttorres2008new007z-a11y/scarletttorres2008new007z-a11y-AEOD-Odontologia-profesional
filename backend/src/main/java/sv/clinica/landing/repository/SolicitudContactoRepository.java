package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.landing.entity.SolicitudContacto;

public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {
}
