package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.api.entity.Permiso;

import java.util.Collection;
import java.util.List;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findByCodigoIn(Collection<String> codigos);

    List<Permiso> findAllByOrderByIdAsc();
}
