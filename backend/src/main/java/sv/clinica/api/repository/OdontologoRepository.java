package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.api.entity.Odontologo;

import java.util.List;
import java.util.Optional;

public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    List<Odontologo> findByActivoTrueOrderByOrdenAscIdAsc();

    Optional<Odontologo> findByIdAndActivoTrue(Long id);

    Optional<Odontologo> findByNombre(String nombre);
}
