package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_contacto")
public class SolicitudContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String email;

    /** Texto libre: el tratamiento que la persona indica en el formulario. */
    @Column(length = 120)
    private String tratamiento;

    @Column(length = 1000)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.NUEVO;

    protected SolicitudContacto() {
    }

    public SolicitudContacto(String nombre, String telefono, String email, String tratamiento, String mensaje,
                             LocalDateTime fecha) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.tratamiento = tratamiento;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getTratamiento() { return tratamiento; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFecha() { return fecha; }
    public EstadoSolicitud getEstado() { return estado; }
}
