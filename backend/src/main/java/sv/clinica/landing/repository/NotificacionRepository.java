package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.landing.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
