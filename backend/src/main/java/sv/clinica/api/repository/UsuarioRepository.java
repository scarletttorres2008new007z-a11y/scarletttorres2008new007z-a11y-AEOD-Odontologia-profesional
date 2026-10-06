package sv.clinica.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Para entrar sirve el usuario o el correo (los dos se guardan en minúsculas). */
    @Query("select u from Usuario u where u.username = :identificador or u.email = :identificador")
    Optional<Usuario> findByUsernameOrEmail(@Param("identificador") String identificador);

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Búsqueda por nombre, usuario o correo y estado. El texto llega ya en minúsculas, con los comodines
     * puestos y con % y _ escapados con "!" (para que busquen esos caracteres tal cual).
     */
    @Query("""
            select u from Usuario u
            where (:activo is null or u.activo = :activo)
              and (:texto is null or lower(u.nombre) like :texto escape '!'
                   or u.username like :texto escape '!' or u.email like :texto escape '!')
            """)
    Page<Usuario> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);

    /** Códigos de los permisos que suman todos los roles del usuario. */
    @Query("select distinct p.codigo from Usuario u join u.roles r join r.permisos p where u.id = :id")
    List<String> findCodigosDePermisos(@Param("id") Long id);

    @Query("select count(distinct u) from Usuario u join u.roles r where r.codigo = 'ADMINISTRADOR' and u.activo = true")
    long contarAdministradoresActivos();
}
