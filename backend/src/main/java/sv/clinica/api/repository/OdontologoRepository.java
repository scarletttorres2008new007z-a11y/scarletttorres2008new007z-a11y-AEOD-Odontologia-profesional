package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.clinica.api.entity.Odontologo;

import java.util.List;
import java.util.Optional;

public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    List<Odontologo> findByActivoTrueOrderByOrdenAscIdAsc();

    Optional<Odontologo> findByIdAndActivoTrue(Long id);

    Optional<Odontologo> findByNombre(String nombre);

    /** Todos, también los desactivados, con su usuario del software (para configurarlos). */
    @EntityGraph(attributePaths = "usuario")
    List<Odontologo> findAllByOrderByOrdenAscIdAsc();

    @Query("select coalesce(max(o.orden), 0) from Odontologo o")
    int ultimoOrden();

    /** El odontólogo vinculado a un usuario del software, si lo hay. */
    Optional<Odontologo> findByUsuarioId(Long usuarioId);
}
