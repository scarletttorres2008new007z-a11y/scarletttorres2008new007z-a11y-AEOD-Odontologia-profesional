package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Rol del personal (ADMINISTRADOR, RECEPCION, ODONTOLOGO, COORDINADOR…) con sus permisos. */
@Entity
@Table(name = "roles")
public class Rol {

    /** Tiene siempre todos los permisos: así nunca se puede perder el acceso a la gestión de usuarios. */
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @ManyToMany
    @JoinTable(name = "rol_permisos",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new LinkedHashSet<>();

    protected Rol() {
    }

    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(codigo);
    }

    public void cambiarPermisos(Collection<Permiso> nuevos) {
        permisos.clear();
        permisos.addAll(nuevos);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Set<Permiso> getPermisos() { return permisos; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Rol otro && id != null && id.equals(otro.getId()));
    }

    @Override
    public int hashCode() {
        return Rol.class.hashCode();
    }
}
