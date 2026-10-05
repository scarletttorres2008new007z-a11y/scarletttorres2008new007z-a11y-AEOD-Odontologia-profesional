package sv.clinica.landing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.clinica.landing.entity.ListaEspera;
import sv.clinica.landing.entity.Odontologo;
import sv.clinica.landing.entity.Tratamiento;

import java.time.LocalDate;
import java.util.List;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    /** Entradas activas a las que podría encajar un hueco liberado (la franja se filtra en el servicio). */
    @Query("""
            select l from ListaEspera l
            where l.estado = sv.clinica.landing.entity.EstadoListaEspera.ACTIVA
              and l.tratamiento = :tratamiento
              and (l.odontologo is null or l.odontologo = :odontologo)
              and :fecha between l.fechaDesde and l.fechaHasta
            order by l.creadaEn
            """)
    List<ListaEspera> findCoincidencias(@Param("tratamiento") Tratamiento tratamiento,
                                        @Param("odontologo") Odontologo odontologo,
                                        @Param("fecha") LocalDate fecha);
}
