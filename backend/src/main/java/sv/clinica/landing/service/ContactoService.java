package sv.clinica.landing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.landing.dto.ContactoRequest;
import sv.clinica.landing.entity.SolicitudContacto;
import sv.clinica.landing.repository.SolicitudContactoRepository;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ContactoService {

    private final SolicitudContactoRepository repository;
    private final Clock clock;

    public ContactoService(SolicitudContactoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Guarda la solicitud con estado inicial NUEVO. */
    @Transactional
    public void registrar(ContactoRequest request) {
        repository.save(new SolicitudContacto(
                request.nombre().trim(),
                request.telefono().trim(),
                request.email().trim(),
                Textos.opcional(request.tratamiento()),
                Textos.opcional(request.mensaje()),
                LocalDateTime.now(clock)));
    }
}
