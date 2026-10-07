package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.clinica.api.entity.Tratamiento;

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

    /** Todos, también los desactivados (para configurarlos). */
    @EntityGraph(attributePaths = "odontologos")
    List<Tratamiento> findAllByOrderByOrdenAscIdAsc();

    @EntityGraph(attributePaths = "odontologos")
    Optional<Tratamiento> findConOdontologosById(Long id);

    @Query("select coalesce(max(t.orden), 0) from Tratamiento t")
    int ultimoOrden();
}
