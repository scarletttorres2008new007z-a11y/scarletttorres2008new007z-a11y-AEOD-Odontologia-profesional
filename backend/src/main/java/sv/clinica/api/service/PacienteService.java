package sv.clinica.api.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.clinica.api.dto.PacienteRequest;
import sv.clinica.api.dto.PacienteResponse;
import sv.clinica.api.dto.PaginaResponse;
import sv.clinica.api.dto.ValidacionPacientes;
import sv.clinica.api.entity.AccionAuditoria;
import sv.clinica.api.entity.EntidadAuditoria;
import sv.clinica.api.entity.OrigenAuditoria;
import sv.clinica.api.entity.Paciente;
import sv.clinica.api.entity.TipoDocumento;
import sv.clinica.api.exception.ConflictoException;
import sv.clinica.api.exception.DatosInvalidosException;
import sv.clinica.api.exception.RecursoNoEncontradoException;
import sv.clinica.api.repository.PacienteRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pacientes: alta, búsqueda, edición y baja. Los permisos de cada operación los comprueba el controller; aquí se
 * revisan y normalizan los datos y cada cambio queda en la auditoría, en la misma transacción.
 * Solo datos administrativos: lo clínico irá en el expediente del paciente (Fase 4).
 */
@Service
public class PacienteService {

    /** Sin 0/O ni 1/I (se confunden al dictarlo) y sin vocales (así el código nunca forma palabras). */
    private static final String LETRAS_DEL_CODIGO = "23456789BCDFGHJKLMNPQRSTVWXYZ";
    private static final int LONGITUD_DEL_CODIGO = 6;
    private static final int EDAD_MAXIMA = 120;
    private static final String DOCUMENTO_REPETIDO =
            "Ya hay un paciente con ese documento. Búscalo en la lista en lugar de darlo de alta otra vez.";

    private final PacienteRepository pacientes;
    private final AuditoriaService auditoria;
    private final Clock clock;
    private final SecureRandom azar = new SecureRandom();

    public PacienteService(PacienteRepository pacientes, AuditoriaService auditoria, Clock clock) {
        this.pacientes = pacientes;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** Busca por nombre, apellidos, documento, teléfono, correo o código; {@code activo} vacío = todos. */
    @Transactional(readOnly = true)
    public PaginaResponse<PacienteResponse> buscar(String texto, Boolean activo, int pagina, int tamano) {
        LocalDate hoy = hoy();
        return PaginaResponse.de(pacientes.findAll(BusquedaDePacientes.de(texto, activo),
                        PageRequest.of(pagina, tamano, Sort.by("apellidos", "nombres", "id"))),
                p -> PacienteResponse.from(p, hoy));
    }

    @Transactional(readOnly = true)
    public PacienteResponse obtener(Long id) {
        return PacienteResponse.from(buscarPaciente(id), hoy());
    }

    @Transactional
    public PacienteResponse crear(PacienteRequest request) {
        Paciente.Datos datos = revisar(request);
        comprobarDocumentoLibre(datos, null);
        LocalDateTime ahora = ahora();
        Paciente paciente = new Paciente(codigoNuevo(), datos, ahora);
        guardar(paciente);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.CREAR, EntidadAuditoria.PACIENTE,
                paciente.getId(), null, datosAuditables(paciente));
        return PacienteResponse.from(paciente, ahora.toLocalDate());
    }

    @Transactional
    public PacienteResponse editar(Long id, PacienteRequest request) {
        Paciente paciente = buscarPaciente(id);
        Paciente.Datos datos = revisar(request);
        if (datos.equals(paciente.datos())) return PacienteResponse.from(paciente, hoy());
        comprobarDocumentoLibre(datos, id);

        Map<String, Object> antes = datosAuditables(paciente);
        LocalDateTime ahora = ahora();
        paciente.editar(datos, ahora);
        guardar(paciente);
        // En la auditoría quedan solo los datos que cambian, con su valor de antes y el de después
        Map<String, Object> despues = datosAuditables(paciente);
        antes.keySet().removeIf(clave -> Objects.equals(antes.get(clave), despues.get(clave)));
        despues.keySet().retainAll(antes.keySet());
        auditoria.registrar(OrigenAuditoria.SOFTWARE, AccionAuditoria.EDITAR, EntidadAuditoria.PACIENTE, id,
                antes, despues);
        return PacienteResponse.from(paciente, ahora.toLocalDate());
    }

    /** Dar de baja no borra nada: el paciente y su historial se conservan y se puede reactivar. */
    @Transactional
    public PacienteResponse cambiarEstado(Long id, boolean activo) {
        Paciente paciente = buscarPaciente(id);
        LocalDateTime ahora = ahora();
        if (paciente.isActivo() == activo) return PacienteResponse.from(paciente, ahora.toLocalDate());
        paciente.cambiarEstado(activo, ahora);
        auditoria.registrar(OrigenAuditoria.SOFTWARE, activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR,
                EntidadAuditoria.PACIENTE, id, Map.of("activo", !activo), Map.of("activo", activo));
        return PacienteResponse.from(paciente, ahora.toLocalDate());
    }

    /** Datos del paciente que se guardan en la auditoría al darlo de alta, con los mismos nombres que en la API. */
    static Map<String, Object> datosAuditables(Paciente p) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", p.getCodigo());
        datos.put("nombres", p.getNombres());
        datos.put("apellidos", p.getApellidos());
        datos.put("tipo_documento", p.getTipoDocumento() == null ? null : p.getTipoDocumento().name());
        datos.put("numero_documento", p.getNumeroDocumento());
        datos.put("fecha_nacimiento", p.getFechaNacimiento() == null ? null : p.getFechaNacimiento().toString());
        datos.put("sexo", p.getSexo() == null ? null : p.getSexo().name());
        datos.put("telefono", p.getTelefono());
        datos.put("email", p.getEmail());
        datos.put("direccion", p.getDireccion());
        datos.put("contacto_emergencia_nombre", p.getContactoEmergenciaNombre());
        datos.put("contacto_emergencia_telefono", p.getContactoEmergenciaTelefono());
        datos.put("observaciones", p.getObservaciones());
        datos.put("activo", p.isActivo());
        return datos;
    }

    /**
     * Revisa lo que las anotaciones de PacienteRequest no pueden (el documento y la fecha de nacimiento) y deja
     * cada dato como se guarda: sin espacios de más, el correo en minúsculas y el teléfono y el documento sin
     * separadores, para que la búsqueda los encuentre se escriban como se escriban.
     */
    private Paciente.Datos revisar(PacienteRequest r) {
        TipoDocumento tipo = r.tipoDocumento();
        String numero = ValidacionPacientes.normalizarDocumento(tipo, r.numeroDocumento());
        if (tipo == null && numero != null) {
            throw new DatosInvalidosException("tipo_documento", "Elige el tipo de documento.");
        }
        if (tipo != null && numero == null) {
            throw new DatosInvalidosException("numero_documento", "Escribe el número del documento.");
        }
        if (tipo != null) {
            String error = ValidacionPacientes.errorDelNumero(tipo, numero);
            if (error != null) throw new DatosInvalidosException("numero_documento", error);
        }

        LocalDate nacimiento = r.fechaNacimiento();
        LocalDate hoy = hoy();
        if (nacimiento != null && nacimiento.isAfter(hoy)) {
            throw new DatosInvalidosException("fecha_nacimiento", "La fecha de nacimiento no puede ser posterior a hoy.");
        }
        if (nacimiento != null && nacimiento.isBefore(hoy.minusYears(EDAD_MAXIMA))) {
            throw new DatosInvalidosException("fecha_nacimiento", "Revisa la fecha de nacimiento.");
        }

        return new Paciente.Datos(nombre(r.nombres()), nombre(r.apellidos()), tipo, numero, nacimiento, r.sexo(),
                telefono(r.telefono()), email(r.email()), Textos.opcional(r.direccion()),
                nombre(r.contactoEmergenciaNombre()), telefono(r.contactoEmergenciaTelefono()),
                Textos.opcional(r.observaciones()));
    }

    private void comprobarDocumentoLibre(Paciente.Datos datos, Long id) {
        if (datos.tipoDocumento() == null) return;
        boolean ocupado = id == null
                ? pacientes.existsByTipoDocumentoAndNumeroDocumento(datos.tipoDocumento(), datos.numeroDocumento())
                : pacientes.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(datos.tipoDocumento(), datos.numeroDocumento(), id);
        if (ocupado) throw new ConflictoException("numero_documento", DOCUMENTO_REPETIDO);
    }

    /** Si dos personas dan de alta a la vez el mismo documento, la base de datos rechaza el segundo. */
    private void guardar(Paciente paciente) {
        try {
            pacientes.saveAndFlush(paciente);
        } catch (DataIntegrityViolationException repetido) {
            String detalle = String.valueOf(repetido.getMostSpecificCause().getMessage());
            if (detalle.contains("uk_paciente_documento")) {
                throw new ConflictoException("numero_documento", DOCUMENTO_REPETIDO);
            }
            throw new ConflictoException("No se ha podido guardar el paciente. Inténtalo de nuevo.");
        }
    }

    /** Código aleatorio de 6 caracteres, como K7M3QX: fácil de dictar e imposible de adivinar. */
    private String codigoNuevo() {
        for (int intento = 0; intento < 10; intento++) {
            StringBuilder codigo = new StringBuilder(LONGITUD_DEL_CODIGO);
            for (int i = 0; i < LONGITUD_DEL_CODIGO; i++) {
                codigo.append(LETRAS_DEL_CODIGO.charAt(azar.nextInt(LETRAS_DEL_CODIGO.length())));
            }
            if (!pacientes.existsByCodigo(codigo.toString())) return codigo.toString();
        }
        throw new IllegalStateException("No se ha podido generar un código de paciente libre.");
    }

    private Paciente buscarPaciente(Long id) {
        return pacientes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El paciente no existe."));
    }

    /** Sin espacios alrededor ni repetidos: "  Ana   María " → "Ana María". */
    private static String nombre(String valor) {
        String limpio = Textos.opcional(valor);
        return limpio == null ? null : limpio.replaceAll("\\s+", " ");
    }

    /** Solo dígitos, con + delante si lo lleva: "+34 600-12.34.56" → "+34600123456". */
    private static String telefono(String valor) {
        String limpio = Textos.opcional(valor);
        if (limpio == null) return null;
        return (limpio.startsWith("+") ? "+" : "") + limpio.replaceAll("\\D", "");
    }

    private static String email(String valor) {
        String limpio = Textos.opcional(valor);
        return limpio == null ? null : limpio.toLowerCase(Locale.ROOT);
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
