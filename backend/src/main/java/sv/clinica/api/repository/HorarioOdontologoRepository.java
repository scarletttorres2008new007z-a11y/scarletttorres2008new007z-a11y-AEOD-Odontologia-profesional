package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.api.entity.HorarioOdontologo;

import java.util.List;

public interface HorarioOdontologoRepository extends JpaRepository<HorarioOdontologo, Long> {

    @Query("select h from HorarioOdontologo h join fetch h.odontologo o where o.activo = true")
    List<HorarioOdontologo> findDeOdontologosActivos();

    List<HorarioOdontologo> findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(Long odontologoId);

    /** Borra a la vez todos los turnos del odontólogo (se sustituyen por los nuevos). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from HorarioOdontologo h where h.odontologo.id = :odontologoId")
    void borrarTurnosDe(@Param("odontologoId") Long odontologoId);
}
