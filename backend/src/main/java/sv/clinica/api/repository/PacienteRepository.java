package sv.clinica.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sv.clinica.api.entity.Paciente;
import sv.clinica.api.entity.TipoDocumento;

/** La búsqueda con filtros se construye en BusquedaDePacientes. */
public interface PacienteRepository extends JpaRepository<Paciente, Long>, JpaSpecificationExecutor<Paciente> {

    boolean existsByCodigo(String codigo);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipo, String numero);

    boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(TipoDocumento tipo, String numero, Long id);
}
