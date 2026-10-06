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

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tratamientos")
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    @Column(name = "descripcion_corta", length = 255)
    private String descripcionCorta;

    /** Precio orientativo "desde". El precio pertenece al tratamiento: no hay tabla de precios. */
    @Column(name = "precio_desde", precision = 10, scale = 2)
    private BigDecimal precioDesde;

    /** Texto para mostrar ("30–45 min", "Según el caso"). */
    @Column(name = "duracion_aproximada", length = 80)
    private String duracionAproximada;

    /**
     * Minutos que ocupa en la agenda la cita que se reserva online
     * (en tratamientos de varias sesiones, la primera cita). Es la que usa el cálculo de disponibilidad.
     */
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    /** Odontólogos que realizan el tratamiento. Vacío = cualquier odontólogo activo. */
    @ManyToMany
    @JoinTable(name = "odontologo_tratamientos",
            joinColumns = @JoinColumn(name = "tratamiento_id"),
            inverseJoinColumns = @JoinColumn(name = "odontologo_id"))
    private Set<Odontologo> odontologos = new LinkedHashSet<>();

    /** Ruta o URL de la imagen. La imagen no se guarda en la base de datos. */
    @Column(length = 500)
    private String imagen;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private int orden;

    protected Tratamiento() {
    }

    public Tratamiento(String nombre, String descripcion, String descripcionCorta, BigDecimal precioDesde,
                       String duracionAproximada, int duracionMinutos, String imagen, int orden) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.descripcionCorta = descripcionCorta;
        this.precioDesde = precioDesde;
        this.duracionAproximada = duracionAproximada;
        this.duracionMinutos = duracionMinutos;
        this.imagen = imagen;
        this.orden = orden;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getDescripcionCorta() { return descripcionCorta; }
    public BigDecimal getPrecioDesde() { return precioDesde; }
    public String getDuracionAproximada() { return duracionAproximada; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public Set<Odontologo> getOdontologos() { return odontologos; }
    public String getImagen() { return imagen; }
    public boolean isActivo() { return activo; }
    public int getOrden() { return orden; }

    public void setActivo(boolean activo) { this.activo = activo; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    /** true si el odontólogo puede hacer este tratamiento (sin asignaciones, cualquiera puede). */
    public boolean loRealiza(Odontologo odontologo) {
        return odontologos.isEmpty() || odontologos.contains(odontologo);
    }
}
