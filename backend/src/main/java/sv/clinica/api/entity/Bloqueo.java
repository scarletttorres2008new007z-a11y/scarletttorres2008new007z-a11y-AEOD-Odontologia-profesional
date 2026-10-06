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
import java.time.LocalTime;

/**
 * Tiempo no disponible: almuerzo, reuniones, mantenimiento, vacaciones, feriados o bloqueos manuales.
 *
 * Cada campo vacío significa "sin límite":
 *   odontologo   vacío → toda la clínica
 *   fecha_inicio / fecha_fin vacías → siempre (si no, solo entre esas fechas, ambas incluidas)
 *   dia_semana   vacío → todos los días (si no, 1 = lunes … 7 = domingo)
 *   hora_inicio / hora_fin vacías → el día entero
 *
 * Ejemplos: almuerzo = dia_semana 1..5, 12:00–13:00 · feriado = fecha_inicio = fecha_fin, sin horas ·
 * vacaciones = odontólogo + rango de fechas, sin horas.
 */
@Entity
@Table(name = "bloqueos")
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoBloqueo tipo;

    @Column(length = 150)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private Odontologo odontologo;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "dia_semana")
    private Integer diaSemana;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(nullable = false)
    private boolean activo = true;

    protected Bloqueo() {
    }

    public Bloqueo(TipoBloqueo tipo, String motivo, Odontologo odontologo, LocalDate fechaInicio, LocalDate fechaFin,
                   Integer diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.tipo = tipo;
        this.motivo = motivo;
        this.odontologo = odontologo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    /** Bloqueo semanal de toda la clínica (por ejemplo, el almuerzo). */
    public static Bloqueo semanal(TipoBloqueo tipo, String motivo, int diaSemana, LocalTime inicio, LocalTime fin) {
        return new Bloqueo(tipo, motivo, null, null, null, diaSemana, inicio, fin);
    }

    /** ¿Se aplica este bloqueo a ese odontólogo ese día? */
    public boolean aplicaA(Odontologo o, LocalDate fecha) {
        return (odontologo == null || odontologo.equals(o)) && aplicaEl(fecha);
    }

    /** ¿Está en vigor ese día (para toda la clínica o para su odontólogo)? */
    public boolean aplicaEl(LocalDate fecha) {
        return activo
                && (fechaInicio == null || !fecha.isBefore(fechaInicio))
                && (fechaFin == null || !fecha.isAfter(fechaFin))
                && (diaSemana == null || diaSemana == fecha.getDayOfWeek().getValue());
    }

    public boolean esDiaCompleto() {
        return horaInicio == null || horaFin == null;
    }

    public TipoBloqueo getTipo() { return tipo; }
    public String getMotivo() { return motivo; }
    public Odontologo getOdontologo() { return odontologo; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
}
