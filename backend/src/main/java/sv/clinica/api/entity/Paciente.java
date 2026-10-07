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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Paciente de la clínica: solo datos personales, de contacto y administrativos.
 * Lo clínico (consultas, diagnósticos, odontograma…) irá en su expediente, nunca aquí.
 * Nunca se borra: se da de baja.
 */
@Entity
@Table(name = "pacientes")
public class Paciente {

    /** Datos que se escriben desde el software, ya revisados y normalizados por PacienteService. */
    public record Datos(
            String nombres,
            String apellidos,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            LocalDate fechaNacimiento,
            Sexo sexo,
            String telefono,
            String email,
            String direccion,
            String contactoEmergenciaNombre,
            String contactoEmergenciaTelefono,
            String observaciones) {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_documento", length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private Sexo sexo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String direccion;

    @Column(name = "contacto_emergencia_nombre", length = 100)
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono", length = 20)
    private String contactoEmergenciaTelefono;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    protected Paciente() {
    }

    public Paciente(String codigo, Datos datos, LocalDateTime ahora) {
        this.codigo = codigo;
        this.creadoEn = ahora;
        editar(datos, ahora);
    }

    public Datos datos() {
        return new Datos(nombres, apellidos, tipoDocumento, numeroDocumento, fechaNacimiento, sexo, telefono, email,
                direccion, contactoEmergenciaNombre, contactoEmergenciaTelefono, observaciones);
    }

    public void editar(Datos datos, LocalDateTime ahora) {
        this.nombres = datos.nombres();
        this.apellidos = datos.apellidos();
        this.tipoDocumento = datos.tipoDocumento();
        this.numeroDocumento = datos.numeroDocumento();
        this.fechaNacimiento = datos.fechaNacimiento();
        this.sexo = datos.sexo();
        this.telefono = datos.telefono();
        this.email = datos.email();
        this.direccion = datos.direccion();
        this.contactoEmergenciaNombre = datos.contactoEmergenciaNombre();
        this.contactoEmergenciaTelefono = datos.contactoEmergenciaTelefono();
        this.observaciones = datos.observaciones();
        this.actualizadoEn = ahora;
    }

    public void cambiarEstado(boolean activo, LocalDateTime ahora) {
        this.activo = activo;
        this.actualizadoEn = ahora;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public Sexo getSexo() { return sexo; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getDireccion() { return direccion; }
    public String getContactoEmergenciaNombre() { return contactoEmergenciaNombre; }
    public String getContactoEmergenciaTelefono() { return contactoEmergenciaTelefono; }
    public String getObservaciones() { return observaciones; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Paciente otro && id != null && id.equals(otro.getId()));
    }

    @Override
    public int hashCode() {
        return Paciente.class.hashCode();
    }
}
