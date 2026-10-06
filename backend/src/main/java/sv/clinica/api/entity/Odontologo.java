package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "odontologos")
public class Odontologo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 150)
    private String especialidad;

    @Column(length = 1000)
    private String descripcion;

    /** Ruta o URL de la foto. La imagen no se guarda en la base de datos. */
    @Column(length = 500)
    private String imagen;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private int orden;

    /** Su usuario del software, para que vea su propia agenda. Vacío = no tiene acceso. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    protected Odontologo() {
    }

    public Odontologo(String nombre, String especialidad, String descripcion, String imagen, int orden) {
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.orden = orden;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEspecialidad() { return especialidad; }
    public String getDescripcion() { return descripcion; }
    public String getImagen() { return imagen; }
    public boolean isActivo() { return activo; }
    public int getOrden() { return orden; }
    public Usuario getUsuario() { return usuario; }

    public void setActivo(boolean activo) { this.activo = activo; }

    /** Los datos que se cambian desde el software (la foto y el orden se conservan). */
    public void editar(String nombre, String especialidad, String descripcion) {
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.descripcion = descripcion;
    }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Odontologo otro && id != null && id.equals(otro.getId()));
    }

    @Override
    public int hashCode() {
        return Odontologo.class.hashCode();
    }
}
