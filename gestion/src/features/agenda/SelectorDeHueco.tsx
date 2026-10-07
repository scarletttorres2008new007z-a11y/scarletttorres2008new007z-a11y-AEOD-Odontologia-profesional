import { useQuery } from '@tanstack/react-query';
import { useId } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { Hueco } from '../../shared/api/tipos';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { buscarHuecos, type PeticionDeHuecos } from './api';
import css from './Agenda.module.css';

interface Props {
  /** null mientras falte el tratamiento o el día. */
  peticion: PeticionDeHuecos | null;
  elegido: Hueco | null;
  alElegir: (hueco: Hueco) => void;
  error?: string;
  /** Al mover una cita: su hora de ahora se señala y no se puede elegir. */
  actual?: { odontologoId: number; fecha: string; horaInicio: string };
  alPedirDiaSiguiente?: () => void;
}

/** Los huecos libres que calcula la API, por odontólogo. Aquí solo se elige uno. */
export function SelectorDeHueco({ peticion, elegido, alElegir, error, actual, alPedirDiaSiguiente }: Props) {
  const nombre = useId();
  const idError = useId();
  const {
    data: huecos,
    error: errorCarga,
    isFetching,
  } = useQuery({
    queryKey: ['huecos', peticion],
    queryFn: ({ signal }) => buscarHuecos(peticion as PeticionDeHuecos, signal),
    enabled: peticion !== null,
  });

  if (!peticion) {
    return <p className={css.nota}>Elige el tratamiento y el día para ver los huecos libres.</p>;
  }
  if (errorCarga) return <Aviso tipo="error">{mensajeDeError(errorCarga)}</Aviso>;
  if (!huecos) return <Cargando texto="Buscando huecos libres…" />;

  const grupos = agrupar(huecos);
  const esActual = (hueco: Hueco) =>
    actual !== undefined &&
    actual.odontologoId === hueco.odontologo_id &&
    actual.fecha === peticion.fecha &&
    actual.horaInicio === hueco.hora_inicio;

  return (
    <fieldset className={css.huecos} aria-busy={isFetching} aria-describedby={error ? idError : undefined}>
      <legend>Huecos libres</legend>
      {grupos.length === 0 ? (
        <div className={css.sinHuecos}>
          <p>
            No queda ningún hueco libre ese día
            {peticion.odontologoId ? ' con ese odontólogo' : ''}.
          </p>
          {alPedirDiaSiguiente && (
            <Boton variante="secundario" pequeno onClick={alPedirDiaSiguiente}>
              Probar el día siguiente
            </Boton>
          )}
        </div>
      ) : (
        grupos.map((grupo) => (
          <div key={grupo.odontologoId} className={css.grupoHuecos}>
            <p className={css.grupoTitulo} id={`${nombre}-${grupo.odontologoId}`}>
              {grupo.odontologo}
            </p>
            <div role="group" aria-labelledby={`${nombre}-${grupo.odontologoId}`} className={css.listaHuecos}>
              {grupo.huecos.map((hueco) => {
                const ahora = esActual(hueco);
                return (
                  <label key={hueco.hora_inicio} className={css.hueco} data-actual={ahora || undefined}>
                    <input
                      type="radio"
                      name={nombre}
                      disabled={ahora}
                      checked={
                        elegido?.odontologo_id === hueco.odontologo_id &&
                        elegido.hora_inicio === hueco.hora_inicio
                      }
                      onChange={() => alElegir(hueco)}
                    />
                    <span>
                      {hueco.hora_inicio}
                      {ahora && ' (la de ahora)'}
                      <span className="visually-hidden"> a {hueco.hora_fin}</span>
                    </span>
                  </label>
                );
              })}
            </div>
          </div>
        ))
      )}
      {error && (
        <p id={idError} className={css.error}>
          {error}
        </p>
      )}
    </fieldset>
  );
}

interface Grupo {
  odontologoId: number;
  odontologo: string;
  huecos: Hueco[];
}

/** Por odontólogo, en el orden en que llegan de la API. */
function agrupar(huecos: Hueco[]): Grupo[] {
  const grupos = new Map<number, Grupo>();
  for (const hueco of huecos) {
    const grupo = grupos.get(hueco.odontologo_id) ?? {
      odontologoId: hueco.odontologo_id,
      odontologo: hueco.odontologo,
      huecos: [],
    };
    grupo.huecos.push(hueco);
    grupos.set(hueco.odontologo_id, grupo);
  }
  return [...grupos.values()];
}
