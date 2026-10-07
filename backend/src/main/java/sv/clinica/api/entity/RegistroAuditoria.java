package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Una operación importante: quién, desde dónde, qué hizo, sobre qué registro y cómo estaba antes y después.
 * Solo se añaden filas; nunca se cambian ni se borran.
 */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vacío cuando no hay una persona del equipo detrás (paciente en la web, el propio sistema). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrigenAuditoria origen;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private AccionAuditoria accion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private EntidadAuditoria entidad;

    @Column(name = "entidad_id", length = 64)
    private String entidadId;

    /** JSON con los datos antes del cambio. */
    @Column(name = "valor_anterior", columnDefinition = "text")
    private String valorAnterior;

    /** JSON con los datos después del cambio. */
    @Column(name = "valor_nuevo", columnDefinition = "text")
    private String valorNuevo;

    @Column(length = 45)
    private String ip;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(Usuario usuario, OrigenAuditoria origen, AccionAuditoria accion, EntidadAuditoria entidad,
                             String entidadId, String valorAnterior, String valorNuevo, String ip, LocalDateTime ahora) {
        this.usuario = usuario;
        this.origen = origen;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.ip = ip;
        this.creadoEn = ahora;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public OrigenAuditoria getOrigen() { return origen; }
    public AccionAuditoria getAccion() { return accion; }
    public EntidadAuditoria getEntidad() { return entidad; }
    public String getEntidadId() { return entidadId; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNuevo() { return valorNuevo; }
    public String getIp() { return ip; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
