package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.landing.entity.Tratamiento;

import java.util.List;
import java.util.Optional;

public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    List<Tratamiento> findByActivoTrueOrderByOrdenAscIdAsc();

    Optional<Tratamiento> findByIdAndActivoTrue(Long id);
}
