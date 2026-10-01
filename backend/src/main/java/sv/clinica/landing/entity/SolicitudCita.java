package sv.clinica.landing.entity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Solicitud de cita: es una preferencia de la persona, no una cita confirmada. */
@Entity
@Table(name = "solicitudes_cita")
public class SolicitudCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    @Column(name = "fecha_preferida", nullable = false)
    private LocalDate fechaPreferida;

    @Column(name = "hora_preferida")
    private LocalTime horaPreferida;

    @Column(length = 1000)
    private String mensaje;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado = EstadoCita.PENDIENTE;

    protected SolicitudCita() {
    }

    public SolicitudCita(String nombre, String telefono, String email, Tratamiento tratamiento,
                         LocalDate fechaPreferida, LocalTime horaPreferida, String mensaje,
                         LocalDateTime fechaSolicitud) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.tratamiento = tratamiento;
        this.fechaPreferida = fechaPreferida;
        this.horaPreferida = horaPreferida;
        this.mensaje = mensaje;
        this.fechaSolicitud = fechaSolicitud;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public Tratamiento getTratamiento() { return tratamiento; }
    public LocalDate getFechaPreferida() { return fechaPreferida; }
    public LocalTime getHoraPreferida() { return horaPreferida; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public EstadoCita getEstado() { return estado; }
}
