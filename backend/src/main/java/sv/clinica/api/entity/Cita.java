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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Cita en la agenda: un odontólogo, un día y un tramo horario.
 * Nunca se borra; cambia de estado (ver {@link EstadoCita}).
 * Los datos del paciente se guardan tal y como los escribió; cuando exista la ficha de paciente
 * (software de clínica) se podrá enlazar sin cambiar esta lógica.
 */
@Entity
@Table(name = "citas",
        uniqueConstraints = @UniqueConstraint(name = "uk_cita_codigo", columnNames = "codigo"),
        indexes = @Index(name = "ix_cita_odontologo_fecha", columnList = "odontologo_id, fecha"))
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia pública y no adivinable (para la futura app, enlaces o QR). Nunca se expone el id interno. */
    @Column(nullable = false, length = 36)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Odontologo odontologo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 1000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrigenCita origen;

    /** Si esta cita sustituye a otra (reprogramación), la anterior. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_anterior_id")
    private Cita citaAnterior;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private LocalDateTime actualizadaEn;

    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "cancelada_por", length = 20)
    private OrigenCita canceladaPor;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    /** Control de concurrencia: dos cambios simultáneos sobre la misma cita no se pisan. */
    @Version
    private long version;

    protected Cita() {
    }

    public Cita(Tratamiento tratamiento, Odontologo odontologo, LocalDate fecha, LocalTime horaInicio, int duracionMinutos,
                String nombre, String telefono, String email, String mensaje,
                EstadoCita estado, OrigenCita origen, LocalDateTime ahora) {
        this.codigo = UUID.randomUUID().toString();
        this.tratamiento = tratamiento;
        this.odontologo = odontologo;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaInicio.plusMinutes(duracionMinutos);
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.mensaje = mensaje;
        this.estado = estado;
        this.origen = origen;
        this.creadaEn = ahora;
        this.actualizadaEn = ahora;
    }

    public LocalDateTime getInicio() {
        return fecha.atTime(horaInicio);
    }

    public void cancelar(OrigenCita por, String motivo, LocalDateTime ahora) {
        this.estado = EstadoCita.CANCELADA;
        this.canceladaPor = por;
        this.motivoCancelacion = motivo;
        this.canceladaEn = ahora;
        this.actualizadaEn = ahora;
    }

    public void marcarReprogramada(LocalDateTime ahora) {
        this.estado = EstadoCita.REPROGRAMADA;
        this.actualizadaEn = ahora;
    }

    public void setCitaAnterior(Cita citaAnterior) { this.citaAnterior = citaAnterior; }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Tratamiento getTratamiento() { return tratamiento; }
    public Odontologo getOdontologo() { return odontologo; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getMensaje() { return mensaje; }
    public EstadoCita getEstado() { return estado; }
    public OrigenCita getOrigen() { return origen; }
    public Cita getCitaAnterior() { return citaAnterior; }
    public LocalDateTime getCanceladaEn() { return canceladaEn; }
    public OrigenCita getCanceladaPor() { return canceladaPor; }
}
