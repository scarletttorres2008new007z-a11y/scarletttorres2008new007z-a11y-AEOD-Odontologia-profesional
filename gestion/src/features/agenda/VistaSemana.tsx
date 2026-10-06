import { Link } from 'react-router';
import type { Agenda, CitaResumen } from '../../shared/api/tipos';
import { conMayuscula, formatearDia } from '../../shared/fechas';
import { Vacio } from '../../shared/ui/Aviso';
import { ESTADOS } from './comun';
import css from './Agenda.module.css';

interface Props {
  agenda: Agenda;
  hoy: string;
  enlace: (cita: CitaResumen) => string;
  /** Dirección de la vista de un día. */
  enlaceDelDia: (fecha: string) => string;
}

/** La semana como lista de citas por día: para ver la carga de trabajo de un vistazo. */
export function VistaSemana({ agenda, hoy, enlace, enlaceDelDia }: Props) {
  if (agenda.odontologos.length === 0 && agenda.solo_su_agenda) {
    return (
      <Vacio>
        Tu usuario todavía no está vinculado a ningún odontólogo, así que no tiene agenda propia. Un
        administrador puede vincularlo.
      </Vacio>
    );
  }
  const conOdontologo = agenda.odontologos.length > 1;

  return (
    <ol className={css.semana}>
      {agenda.dias.map((dia) => {
        const delDia = agenda.citas.filter((c) => c.fecha === dia.fecha);
        return (
          <li key={dia.fecha} className={css.diaSemana} data-hoy={dia.fecha === hoy || undefined}>
            <h3 className={css.diaTitulo}>
              <Link to={enlaceDelDia(dia.fecha)}>{conMayuscula(formatearDia(dia.fecha))}</Link>
            </h3>
            <p className={css.horario}>
              {dia.fecha === hoy && <span className={css.hoy}>Hoy · </span>}
              {dia.apertura ? `${dia.apertura} a ${dia.cierre}` : 'Cerrado'}
              {delDia.length > 0 && ` · ${delDia.length} ${delDia.length === 1 ? 'cita' : 'citas'}`}
            </p>
            {delDia.length > 0 ? (
              <ul className={css.citasDelDia}>
                {delDia.map((cita) => (
                  <li key={cita.id}>
                    <Link to={enlace(cita)} className={css.citaSemana} data-estado={cita.estado}>
                      <span className={css.citaLinea}>
                        <span className={css.citaHora}>{cita.hora_inicio}</span>{' '}
                        <span className={css.citaNombre}>{cita.nombre}</span>
                      </span>
                      <span className={css.citaDetalle}>
                        {cita.tratamiento}
                        {conOdontologo && ` · ${cita.odontologo}`}
                      </span>
                      <span className={css.citaDetalle}>
                        {ESTADOS[cita.estado].texto}
                        {!cita.paciente_id && ' · Sin ficha'}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            ) : (
              dia.apertura && <p className={css.nota}>Sin citas.</p>
            )}
          </li>
        );
      })}
    </ol>
  );
}
