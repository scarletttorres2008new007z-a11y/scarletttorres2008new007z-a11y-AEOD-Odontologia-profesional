package sv.clinica.landing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;

/**
 * Turno de un odontólogo un día de la semana (1 = lunes … 7 = domingo).
 * Puede haber varios turnos el mismo día (por ejemplo, mañana y tarde).
 */
@Entity
@Table(name = "horarios_odontologo")
public class HorarioOdontologo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Odontologo odontologo;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    protected HorarioOdontologo() {
    }

    public HorarioOdontologo(Odontologo odontologo, int diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.odontologo = odontologo;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public Odontologo getOdontologo() { return odontologo; }
    public int getDiaSemana() { return diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
}
