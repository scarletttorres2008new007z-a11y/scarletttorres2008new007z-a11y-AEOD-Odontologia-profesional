package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.clinica.landing.entity.Tratamiento;

import java.util.List;
import java.util.Optional;

public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    @EntityGraph(attributePaths = "odontologos")
    List<Tratamiento> findByActivoTrueOrderByOrdenAscIdAsc();

    @EntityGraph(attributePaths = "odontologos")
    Optional<Tratamiento> findByIdAndActivoTrue(Long id);

    @EntityGraph(attributePaths = "odontologos")
    Optional<Tratamiento> findByNombre(String nombre);

    boolean existsByOdontologosIsNotEmpty();
}
