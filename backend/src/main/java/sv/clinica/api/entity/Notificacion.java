package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Bandeja de avisos pendientes ("outbox"). Se escribe en la misma transacción que la cita,
 * así nunca se pierde un aviso ni se avisa de una cita que no llegó a guardarse.
 * Hoy no se envía nada: un futuro proceso leerá las PENDIENTE y las mandará por el canal que exista.
 */
@Entity
@Table(name = "notificaciones", indexes = @Index(name = "ix_notificacion_estado", columnList = "estado"))
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private DestinatarioNotificacion destinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_espera_id")
    private ListaEspera listaEspera;

    /** Resumen legible del aviso. */
    @Column(length = 500)
    private String detalle;

    /** Canal por el que se envió (correo, WhatsApp, app…). Vacío mientras esté pendiente. */
    @Column(length = 20)
    private String canal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "enviada_en")
    private LocalDateTime enviadaEn;

    protected Notificacion() {
    }

    public Notificacion(TipoNotificacion tipo, DestinatarioNotificacion destinatario, Odontologo odontologo,
                        Cita cita, ListaEspera listaEspera, String detalle, LocalDateTime ahora) {
        this.tipo = tipo;
        this.destinatario = destinatario;
        this.odontologo = odontologo;
        this.cita = cita;
        this.listaEspera = listaEspera;
        this.detalle = detalle;
        this.creadaEn = ahora;
    }

    public TipoNotificacion getTipo() { return tipo; }
    public DestinatarioNotificacion getDestinatario() { return destinatario; }
    public EstadoNotificacion getEstado() { return estado; }
    public String getDetalle() { return detalle; }
}
