package sv.clinica.landing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalTime;

/**
 * Horario de apertura de la clínica por día de la semana (1 = lunes … 7 = domingo).
 * Un día sin fila está cerrado.
 */
@Entity
@Table(name = "horarios_clinica", uniqueConstraints = @UniqueConstraint(name = "uk_horario_clinica_dia", columnNames = "dia_semana"))
public class HorarioClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    protected HorarioClinica() {
    }

    public HorarioClinica(int diaSemana, LocalTime horaApertura, LocalTime horaCierre) {
        this.diaSemana = diaSemana;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
    }

    public int getDiaSemana() { return diaSemana; }
    public LocalTime getHoraApertura() { return horaApertura; }
    public LocalTime getHoraCierre() { return horaCierre; }
}
