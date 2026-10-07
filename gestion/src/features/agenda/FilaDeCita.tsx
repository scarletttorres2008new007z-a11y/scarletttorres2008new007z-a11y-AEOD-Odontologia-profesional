import { Link } from 'react-router';
import type { CitaResumen } from '../../shared/api/tipos';
import { conMayuscula, formatearDia, formatearDiaCorto } from '../../shared/fechas';
import { Insignia } from '../../shared/ui/Insignia';
import css from './Agenda.module.css';
import { enlaceDeCita, ESTADOS } from './comun';

/** Una cita en una línea; conDia pone el día encima de la hora (abreviado a la vista, entero al leerlo). */
export function FilaDeCita({
  cita,
  conDia = false,
  detalle,
}: {
  cita: CitaResumen;
  conDia?: boolean;
  detalle: string;
}) {
  return (
    <Link to={enlaceDeCita(cita)} className={css.fila}>
      <span className={css.filaHora}>
        {conDia && (
          <>
            <span aria-hidden="true" className={css.filaDia}>
              {formatearDiaCorto(cita.fecha)}
            </span>
            <span className="visually-hidden">{conMayuscula(formatearDia(cita.fecha))},</span>{' '}
          </>
        )}
        <span>{cita.hora_inicio}</span>
      </span>
      <span className={css.filaTexto}>
        <span className={css.citaNombre}>{cita.nombre}</span>
        <span className={css.citaDetalle}>{detalle}</span>
      </span>
      <Insignia tono={ESTADOS[cita.estado].tono}>{ESTADOS[cita.estado].texto}</Insignia>
    </Link>
  );
}

/** Varias citas de distintos días, una por línea: para avisar de las que hay que revisar. */
export function ListaDeCitas({ citas }: { citas: CitaResumen[] }) {
  return (
    <ul className={css.filas}>
      {citas.map((cita) => (
        <li key={cita.id}>
          <FilaDeCita cita={cita} conDia detalle={`${cita.tratamiento} · ${cita.odontologo}`} />
        </li>
      ))}
    </ul>
  );
}
