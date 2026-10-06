package sv.clinica.api.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Estructura preparada para la futura lista de espera ("avísame si se libera un horario").
 * Todavía no hay pantalla para apuntarse: cuando se libera un hueco, el backend ya busca
 * las entradas que encajan y deja una notificación pendiente para cada una.
 */
@Entity
@Table(name = "lista_espera")
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    /** Vacío = le vale cualquier odontólogo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    /** Vacío = cualquier hora. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private Franja franja;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoListaEspera estado = EstadoListaEspera.ACTIVA;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrigenCita origen;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    protected ListaEspera() {
    }

    public ListaEspera(Tratamiento tratamiento, Odontologo odontologo, LocalDate fechaDesde, LocalDate fechaHasta,
                       Franja franja, String nombre, String telefono, String email, OrigenCita origen, LocalDateTime ahora) {
        this.tratamiento = tratamiento;
        this.odontologo = odontologo;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.franja = franja;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.origen = origen;
        this.creadaEn = ahora;
    }

    public Long getId() { return id; }
    public Franja getFranja() { return franja; }
    public String getNombre() { return nombre; }
    public EstadoListaEspera getEstado() { return estado; }
}
