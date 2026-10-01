package sv.clinica.landing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.landing.dto.OdontologoResponse;
import sv.clinica.landing.exception.RecursoNoEncontradoException;
import sv.clinica.landing.repository.OdontologoRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OdontologoService {

    private final OdontologoRepository repository;

    public OdontologoService(OdontologoRepository repository) {
        this.repository = repository;
    }

    public List<OdontologoResponse> listarActivos() {
        return repository.findByActivoTrueOrderByOrdenAscIdAsc().stream()
                .map(OdontologoResponse::from)
                .toList();
    }

    public OdontologoResponse obtener(Long id) {
        return repository.findByIdAndActivoTrue(id)
                .map(OdontologoResponse::from)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un odontólogo con id " + id + "."));
    }
}
