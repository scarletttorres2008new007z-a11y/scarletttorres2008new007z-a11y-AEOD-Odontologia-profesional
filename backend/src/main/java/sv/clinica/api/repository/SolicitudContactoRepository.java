package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.api.entity.SolicitudContacto;

public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {
}
