package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.Sesion;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    @Query("select s from Sesion s join fetch s.usuario where s.refreshTokenHash = :hash")
    Optional<Sesion> findByRefreshTokenHash(@Param("hash") String hash);

    @Query("select s from Sesion s join fetch s.usuario where s.id = :id")
    Optional<Sesion> findConUsuario(@Param("id") Long id);

    /** Cierra todas las sesiones abiertas de un usuario, salvo (opcionalmente) una. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Sesion s set s.revocadaEn = :ahora
            where s.usuario.id = :usuarioId and s.revocadaEn is null and (:excepto is null or s.id <> :excepto)
            """)
    int revocarDelUsuario(@Param("usuarioId") Long usuarioId, @Param("excepto") Long excepto,
                          @Param("ahora") LocalDateTime ahora);
}
