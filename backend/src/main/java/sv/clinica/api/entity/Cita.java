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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Cita en la agenda: un odontólogo, un día y un tramo horario.
 * Nunca se borra; cambia de estado (ver {@link EstadoCita}).
 * Los datos de contacto se guardan tal y como llegaron (en la web, los que escribió el paciente). La ficha del
 * paciente se enlaza aparte: las citas del software la llevan desde el principio y las de la web, cuando recepción
 * las vincula.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

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

    @Column(length = 150)
    private String email;

    /** Lo que escribió el paciente al reservar en la web. */
    @Column(length = 1000)
    private String mensaje;

    @Column(name = "notas_internas", length = 1000)
    private String notasInternas;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrigenCita origen;

    /** Quién la dio desde el software (vacío en las de la web). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por_usuario_id")
    private Usuario creadaPor;

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

    /** Cita dada desde el software a un paciente con ficha: los datos de contacto salen de ella. */
    public static Cita paraPaciente(Paciente paciente, Tratamiento tratamiento, Odontologo odontologo, LocalDate fecha,
                                    LocalTime horaInicio, int duracionMinutos, String notasInternas, EstadoCita estado,
                                    Usuario creadaPor, LocalDateTime ahora) {
        Cita cita = new Cita(tratamiento, odontologo, fecha, horaInicio, duracionMinutos,
                paciente.getNombres() + " " + paciente.getApellidos(), paciente.getTelefono(), paciente.getEmail(),
                null, estado, OrigenCita.SOFTWARE, ahora);
        cita.paciente = paciente;
        cita.notasInternas = notasInternas;
        cita.creadaPor = creadaPor;
        return cita;
    }

    public LocalDateTime getInicio() {
        return fecha.atTime(horaInicio);
    }

    /** Estados a los que puede pasar ahora: en consulta, completada y no asistió, solo desde el día de la cita. */
    public List<EstadoCita> estadosSiguientes(LocalDate hoy) {
        return estado.siguientes().stream()
                .filter(siguiente -> !EstadoCita.DEL_DIA.contains(siguiente) || !fecha.isAfter(hoy))
                .toList();
    }

    public int getDuracionMinutos() {
        return (int) Duration.between(horaInicio, horaFin).toMinutes();
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

    /** Confirmar, en consulta, completada o no asistió. Las reglas de qué cambio vale están en CitaService. */
    public void cambiarEstado(EstadoCita nuevo, LocalDateTime ahora) {
        this.estado = nuevo;
        this.actualizadaEn = ahora;
    }

    public void vincularPaciente(Paciente paciente, LocalDateTime ahora) {
        this.paciente = paciente;
        this.actualizadaEn = ahora;
    }

    public void cambiarNotasInternas(String notas, LocalDateTime ahora) {
        this.notasInternas = notas;
        this.actualizadaEn = ahora;
    }

    /** Al reprogramar, la nueva cita conserva la ficha del paciente y las notas de la anterior. */
    public void heredarDe(Cita anterior, Usuario creadaPor) {
        this.citaAnterior = anterior;
        this.paciente = anterior.paciente;
        this.notasInternas = anterior.notasInternas;
        this.creadaPor = creadaPor;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Tratamiento getTratamiento() { return tratamiento; }
    public Odontologo getOdontologo() { return odontologo; }
    public Paciente getPaciente() { return paciente; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getMensaje() { return mensaje; }
    public String getNotasInternas() { return notasInternas; }
    public EstadoCita getEstado() { return estado; }
    public OrigenCita getOrigen() { return origen; }
    public Usuario getCreadaPor() { return creadaPor; }
    public Cita getCitaAnterior() { return citaAnterior; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getActualizadaEn() { return actualizadaEn; }
    public LocalDateTime getCanceladaEn() { return canceladaEn; }
    public OrigenCita getCanceladaPor() { return canceladaPor; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
}
