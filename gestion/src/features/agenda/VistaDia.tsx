import type { CSSProperties } from 'react';
import { Link } from 'react-router';
import type { Agenda, CitaResumen } from '../../shared/api/tipos';
import { horaDe, minutosDe } from '../../shared/fechas';
import { Vacio } from '../../shared/ui/Aviso';
import { BLOQUEOS, ESTADOS } from './comun';
import css from './Agenda.module.css';

/** Cada fila de la rejilla son 15 minutos, lo mismo que cada tramo que reserva una cita. */
const PASO = 15;

interface Props {
  agenda: Agenda;
  /** Dirección que abre la cita sin salir de la agenda. */
  enlace: (cita: CitaResumen) => string;
}

/**
 * Un día con una columna por odontólogo. Solo pinta lo que devuelve la API (turnos, bloqueos y citas);
 * qué hueco está libre para una cita nueva lo calcula la API al darla.
 */
export function VistaDia({ agenda, enlace }: Props) {
  const dia = agenda.dias[0];
  if (!dia) return null;
  const { odontologos, citas } = agenda;

  if (odontologos.length === 0) {
    return (
      <Vacio>
        {agenda.solo_su_agenda
          ? 'Tu usuario todavía no está vinculado a ningún odontólogo, así que no tiene agenda propia. Se vincula en «Odontólogos».'
          : 'No hay odontólogos con agenda este día.'}
      </Vacio>
    );
  }
  if (!dia.apertura && citas.length === 0) {
    return <Vacio>La clínica no abre este día.</Vacio>;
  }

  // De la primera a la última hora con algo que enseñar, en horas enteras
  const inicios = [dia.apertura, ...citas.map((c) => c.hora_inicio)].filter(Boolean) as string[];
  const finales = [dia.cierre, ...citas.map((c) => c.hora_fin)].filter(Boolean) as string[];
  const inicio = Math.floor(Math.min(...inicios.map(minutosDe)) / 60) * 60;
  const fin = Math.ceil(Math.max(...finales.map(minutosDe)) / 60) * 60;
  const filas = (fin - inicio) / PASO;
  const fila = (hora: string) => Math.min(Math.max((minutosDe(hora) - inicio) / PASO, 0), filas) + 1;
  const tramo = (desde: string | undefined, hasta: string | undefined): CSSProperties => ({
    gridRow: `${desde ? fila(desde) : 1} / ${hasta ? fila(hasta) : filas + 1}`,
  });
  const horas = Array.from({ length: (fin - inicio) / 60 }, (_, i) => horaDe(inicio + i * 60));

  return (
    <div className={css.rejillaCaja}>
      <div
        className={css.rejilla}
        style={{ '--columnas': odontologos.length, '--filas': filas } as CSSProperties}
      >
        <div className={css.esquina} aria-hidden="true" />
        {odontologos.map((odontologo) => (
          <div key={odontologo.id} className={css.cabeceraColumna} aria-hidden="true">
            {odontologo.nombre}
          </div>
        ))}

        <div className={css.horas} aria-hidden="true">
          {horas.map((hora) => (
            <span key={hora} style={tramo(hora, horaDe(minutosDe(hora) + 60))}>
              {hora}
            </span>
          ))}
        </div>

        {odontologos.map((odontologo) => {
          const suyas = citas.filter((c) => c.odontologo_id === odontologo.id);
          return (
            <section
              key={odontologo.id}
              className={css.columna}
              aria-label={`Agenda de ${odontologo.nombre}`}
            >
              {agenda.turnos
                .filter((t) => t.odontologo_id === odontologo.id)
                .map((t) => (
                  <div key={t.hora_inicio} className={css.turno} style={tramo(t.hora_inicio, t.hora_fin)} />
                ))}
              {agenda.bloqueos
                .filter((b) => b.odontologo_id === undefined || b.odontologo_id === odontologo.id)
                .map((b, posicion) => (
                  <div
                    key={`${b.tipo}-${b.hora_inicio ?? 'dia'}-${posicion}`}
                    className={css.bloqueo}
                    style={tramo(b.hora_inicio, b.hora_fin)}
                  >
                    {BLOQUEOS[b.tipo]}
                    {b.motivo && b.motivo !== BLOQUEOS[b.tipo] ? `: ${b.motivo}` : ''}
                  </div>
                ))}
              {suyas.length === 0 ? (
                <p className="visually-hidden">Sin citas.</p>
              ) : (
                suyas.map((cita) => (
                  <BloqueDeCita
                    key={cita.id}
                    cita={cita}
                    enlace={enlace(cita)}
                    style={tramo(cita.hora_inicio, cita.hora_fin)}
                  />
                ))
              )}
            </section>
          );
        })}
      </div>
    </div>
  );
}

function BloqueDeCita({ cita, enlace, style }: { cita: CitaResumen; enlace: string; style: CSSProperties }) {
  const corta = minutosDe(cita.hora_fin) - minutosDe(cita.hora_inicio) <= 30;
  return (
    <Link
      to={enlace}
      className={css.cita}
      data-estado={cita.estado}
      data-corta={corta || undefined}
      style={style}
    >
      <span className={css.citaLinea}>
        <span className={css.citaHora}>{cita.hora_inicio}</span>
        <span className="visually-hidden"> a {cita.hora_fin}, </span>{' '}
        <span className={css.citaNombre}>{cita.nombre}</span>
      </span>
      <span className={css.citaDetalle}>
        {cita.tratamiento} · {ESTADOS[cita.estado].texto}
        {!cita.paciente_id && ' · Sin ficha'}
      </span>
    </Link>
  );
}
