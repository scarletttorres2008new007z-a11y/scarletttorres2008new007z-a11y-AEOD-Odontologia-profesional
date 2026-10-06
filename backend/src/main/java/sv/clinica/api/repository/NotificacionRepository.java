package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.api.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
