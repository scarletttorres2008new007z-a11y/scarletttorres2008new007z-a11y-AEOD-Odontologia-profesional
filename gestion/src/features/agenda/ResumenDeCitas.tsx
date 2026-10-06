import { useQuery } from '@tanstack/react-query';
import { useId } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../../shared/api/errores';
import { hoyEnMadrid } from '../../shared/fechas';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import p from '../../shared/ui/pagina.module.css';
import { buscarCitas, obtenerAgenda } from './api';
import css from './Agenda.module.css';
import { FilaDeCita } from './FilaDeCita';

const POR_CONFIRMAR = 5;

/** Para el inicio: las citas de hoy y las reservas que aún hay que confirmar. */
export function ResumenDeCitas() {
  const hoy = hoyEnMadrid();
  const idHoy = useId();
  const idPendientes = useId();
  // Misma consulta que la agenda del día: comparten caché y se refrescan juntas
  const deHoy = useQuery({
    queryKey: ['agenda', hoy, 1, null],
    queryFn: ({ signal }) => obtenerAgenda(hoy, 1, null, signal),
    refetchInterval: 60_000,
  });
  const pendientes = useQuery({
    queryKey: ['citas', 'por-confirmar', hoy],
    queryFn: ({ signal }) => buscarCitas({ estado: 'PENDIENTE', desde: hoy, tamano: POR_CONFIRMAR }, signal),
    refetchInterval: 60_000,
  });
  const totalPendientes = pendientes.data?.total_elementos ?? 0;

  return (
    <div className={css.resumen}>
      <section className={p.tarjeta} aria-labelledby={idHoy}>
        <div className={css.cabeceraSeccion}>
          <h2 id={idHoy}>Citas de hoy</h2>
          <Link to="/agenda">Abrir la agenda</Link>
        </div>
        {deHoy.error ? (
          <Aviso tipo="error">{mensajeDeError(deHoy.error)}</Aviso>
        ) : !deHoy.data ? (
          <Cargando texto="Cargando las citas de hoy…" />
        ) : deHoy.data.citas.length === 0 ? (
          <p className={css.nota}>No hay citas para hoy.</p>
        ) : (
          <ul className={css.filas}>
            {deHoy.data.citas.map((cita) => (
              <li key={cita.id}>
                <FilaDeCita cita={cita} detalle={`${cita.tratamiento} · ${cita.odontologo}`} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className={p.tarjeta} aria-labelledby={idPendientes}>
        <div className={css.cabeceraSeccion}>
          <h2 id={idPendientes}>
            Por confirmar
            {totalPendientes > 0 && (
              <>
                {' '}
                <span className={css.contador}>({totalPendientes})</span>
              </>
            )}
          </h2>
        </div>
        <p className={css.nota}>
          Reservas de la web, de hoy en adelante, que la clínica aún no ha confirmado.
        </p>
        {pendientes.error ? (
          <Aviso tipo="error">{mensajeDeError(pendientes.error)}</Aviso>
        ) : !pendientes.data ? (
          <Cargando texto="Cargando…" />
        ) : pendientes.data.contenido.length === 0 ? (
          <p className={css.nota}>No hay ninguna cita por confirmar.</p>
        ) : (
          <>
            <ul className={css.filas}>
              {pendientes.data.contenido.map((cita) => (
                <li key={cita.id}>
                  <FilaDeCita
                    cita={cita}
                    conDia
                    detalle={`${cita.tratamiento}${cita.paciente_id ? '' : ' · Sin ficha'}`}
                  />
                </li>
              ))}
            </ul>
            {totalPendientes > POR_CONFIRMAR && (
              <p className={css.nota}>
                Y {totalPendientes - POR_CONFIRMAR} más: las verás en la agenda con el color de «Por
                confirmar».
              </p>
            )}
          </>
        )}
      </section>
    </div>
  );
}
