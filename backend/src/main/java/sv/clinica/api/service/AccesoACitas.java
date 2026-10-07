package sv.clinica.api.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import sv.clinica.api.entity.Cita;
import sv.clinica.api.entity.Odontologo;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.OdontologoRepository;
import sv.clinica.api.security.Permisos;
import sv.clinica.api.security.UsuarioAutenticado;

/**
 * Qué agendas puede ver quien hace la petición. Con «citas.ver_todas», todas; sin él, solo la del odontólogo
 * vinculado a su usuario, y ninguna si no tiene. Una cita de otra agenda se trata como si no existiera (404), así
 * nadie puede ver citas ajenas escribiendo su número a mano.
 */
@Component
public class AccesoACitas {

    static final String CITA_NO_EXISTE = "La cita no existe.";

    /**
     * @param todas         ve las agendas de todos los odontólogos
     * @param odontologoId  si no ve todas, el odontólogo vinculado a su usuario (vacío = ninguno)
     */
    public record Alcance(boolean todas, Long odontologoId) {

        public boolean puedeVer(Odontologo odontologo) {
            return todas || (odontologoId != null && odontologoId.equals(odontologo.getId()));
        }

        /** Para filtrar listados: el odontólogo pedido, o el suyo si solo ve su agenda (-1 si no tiene ninguno). */
        public Long odontologoParaFiltrar(Long pedido) {
            if (todas) return pedido;
            return odontologoId == null ? -1L : odontologoId;
        }
    }

    private final OdontologoRepository odontologos;

    public AccesoACitas(OdontologoRepository odontologos) {
        this.odontologos = odontologos;
    }

    public Alcance delUsuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return new Alcance(false, null);
        }
        if (usuario.tienePermiso(Permisos.CITAS_VER_TODAS)) return new Alcance(true, null);
        return new Alcance(false, odontologos.findByUsuarioId(usuario.id()).map(Odontologo::getId).orElse(null));
    }

    /** La cita, si quien pregunta puede verla; si no, 404 como si no existiera. */
    public Cita comprobar(Cita cita) {
        if (!delUsuarioActual().puedeVer(cita.getOdontologo())) throw new RecursoNoEncontradoException(CITA_NO_EXISTE);
        return cita;
    }
}
