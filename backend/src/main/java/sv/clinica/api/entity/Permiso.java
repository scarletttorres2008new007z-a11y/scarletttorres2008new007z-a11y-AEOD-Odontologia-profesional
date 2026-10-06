package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Acción concreta que un rol puede tener permitida, con código "modulo.accion" (usuarios.crear, auditoria.ver…).
 * Los permisos los crean las migraciones de cada fase; el backend comprueba uno en cada endpoint privado.
 */
@Entity
@Table(name = "permisos")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String modulo;

    @Column(nullable = false, length = 255)
    private String descripcion;

    protected Permiso() {
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getModulo() { return modulo; }
    public String getDescripcion() { return descripcion; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Permiso otro && id != null && id.equals(otro.getId()));
    }

    @Override
    public int hashCode() {
        return Permiso.class.hashCode();
    }
}
