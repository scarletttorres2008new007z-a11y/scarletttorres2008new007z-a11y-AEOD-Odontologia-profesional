package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cada tramo de {@value #MINUTOS_TRAMO} minutos ocupado por una cita activa.
 *
 * La clave única (odontólogo, fecha, hora) es la garantía contra la doble reserva en la propia base de datos:
 * si dos personas confirman a la vez citas que se solapan, solo una inserción puede ganar.
 * Al cancelar o reprogramar se borran estos tramos (la cita se conserva) y el hueco queda libre.
 */
@Entity
@Table(name = "agenda_ocupacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_agenda_ocupacion", columnNames = {"odontologo_id", "fecha", "hora"}))
public class OcupacionAgenda {

    public static final int MINUTOS_TRAMO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Odontologo odontologo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    protected OcupacionAgenda() {
    }

    public OcupacionAgenda(Cita cita, LocalTime hora) {
        this.cita = cita;
        this.odontologo = cita.getOdontologo();
        this.fecha = cita.getFecha();
        this.hora = hora;
    }
}
