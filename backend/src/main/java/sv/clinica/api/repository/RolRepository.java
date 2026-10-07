package sv.clinica.api.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.Rol;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByCodigo(String codigo);

    List<Rol> findByCodigoIn(Collection<String> codigos);

    List<Rol> findAllByOrderByIdAsc();

    /**
     * Bloquea la fila del rol hasta el final de la transacción. Los cambios que pueden dejar la clínica sin
     * administradores lo usan para no ejecutarse a la vez (dos administradores desactivándose mutuamente).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.codigo = :codigo")
    Optional<Rol> bloquear(@Param("codigo") String codigo);

    /** Cuántos usuarios tiene cada rol: filas [rol_id, total]. */
    @Query("select r.id, count(u) from Usuario u join u.roles r group by r.id")
    List<Object[]> contarUsuariosPorRol();
}
