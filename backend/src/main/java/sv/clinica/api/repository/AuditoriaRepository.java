package sv.clinica.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.RegistroAuditoria;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    /** Filtros opcionales: los que llegan vacíos no se aplican. Las fechas son [desde, hasta). */
    @EntityGraph(attributePaths = "usuario")
    @Query("""
            select a from RegistroAuditoria a
            where (:entidad is null or a.entidad = :entidad)
              and (:entidadId is null or a.entidadId = :entidadId)
              and (:usuarioId is null or a.usuario.id = :usuarioId)
              and (:accion is null or a.accion = :accion)
              and (:desde is null or a.creadoEn >= :desde)
              and (:hasta is null or a.creadoEn < :hasta)
            """)
    Page<RegistroAuditoria> buscar(@Param("entidad") EntidadAuditoria entidad, @Param("entidadId") String entidadId,
                                   @Param("usuarioId") Long usuarioId, @Param("accion") AccionAuditoria accion,
                                   @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                   Pageable pageable);
}
