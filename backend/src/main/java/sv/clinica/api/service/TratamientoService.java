package sv.clinica.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.TratamientoResponse;
import sv.clinica.api.entity.Tratamiento;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.TratamientoRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TratamientoService {

    private final TratamientoRepository repository;

    public TratamientoService(TratamientoRepository repository) {
        this.repository = repository;
    }

    public List<TratamientoResponse> listarActivos() {
        return repository.findByActivoTrueOrderByOrdenAscIdAsc().stream()
                .map(TratamientoResponse::from)
                .toList();
    }

    public TratamientoResponse obtener(Long id) {
        return TratamientoResponse.from(buscarActivo(id));
    }

    /** Usado también por CitaService para comprobar que el tratamiento existe. */
    public Tratamiento buscarActivo(Long id) {
        return repository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un tratamiento con id " + id + "."));
    }
}
