import { useQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { Bloqueo } from '../../shared/api/tipos';
import { useTitulo } from '../../shared/hooks';
import { Aviso, Cargando, Vacio } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import { BLOQUEOS } from '../agenda/comun';
import { CLAVES, listarBloqueos, quitarBloqueo } from './api';
import { AvisoDeResultado } from './AvisoDeResultado';
import { cuandoDelBloqueo, horasDelBloqueo, type Resultado } from './comun';
import { DialogoBloqueo } from './DialogoBloqueo';
import { DialogoConfirmacion } from './DialogoConfirmacion';

type Accion = { tipo: 'crear' } | { tipo: 'editar' | 'quitar'; bloqueo: Bloqueo };

export function PaginaBloqueos() {
  useTitulo('Bloqueos');
  const bloqueos = useQuery({ queryKey: CLAVES.bloqueos, queryFn: ({ signal }) => listarBloqueos(signal) });
  const [accion, setAccion] = useState<Accion | null>(null);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  const abrir = (nueva: Accion) => {
    setResultado(null);
    setAccion(nueva);
  };
  const cerrar = () => setAccion(null);
  const terminar = (hecho: Resultado) => {
    setAccion(null);
    setResultado(hecho);
  };

  const acciones = (b: Bloqueo): ReactNode[] => {
    const nombre = `${BLOQUEOS[b.tipo]}, ${cuandoDelBloqueo(b).toLowerCase()}`;
    return [
      <Boton key="editar" variante="texto" pequeno onClick={() => abrir({ tipo: 'editar', bloqueo: b })}>
        Editar<TextoOculto>{nombre}</TextoOculto>
      </Boton>,
      <Boton key="quitar" variante="texto" pequeno onClick={() => abrir({ tipo: 'quitar', bloqueo: b })}>
        Quitar<TextoOculto>{nombre}</TextoOculto>
      </Boton>,
    ];
  };

  return (
    <div className={p.pagina}>
      <div className={p.cabecera}>
        <div>
          <h1>Bloqueos</h1>
          <p className={p.entradilla}>
            Tiempo en que no se dan citas, ni en la web ni desde la agenda: festivos, vacaciones, reuniones,
            capacitaciones… Para toda la clínica o para un odontólogo.
          </p>
        </div>
        <div className={p.acciones}>
          <Boton onClick={() => abrir({ tipo: 'crear' })}>Nuevo bloqueo</Boton>
        </div>
      </div>

      {resultado && <AvisoDeResultado resultado={resultado} />}
      {bloqueos.error && <Aviso tipo="error">{mensajeDeError(bloqueos.error)}</Aviso>}
      {bloqueos.isPending && <Cargando texto="Cargando bloqueos…" />}
      {bloqueos.data?.length === 0 && <Vacio>No hay ningún bloqueo en vigor.</Vacio>}
      {bloqueos.data && bloqueos.data.length > 0 && (
        <div className={p.tablaCaja}>
          <table className={p.tabla}>
            <caption className="visually-hidden">Bloqueos en vigor</caption>
            <thead>
              <tr>
                <th scope="col">Bloqueo</th>
                <th scope="col">Para</th>
                <th scope="col">Cuándo</th>
                <th scope="col">Horas</th>
                <th scope="col">
                  <span className="visually-hidden">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {bloqueos.data.map((b) => (
                <tr key={b.id}>
                  <td>
                    <Insignia>{BLOQUEOS[b.tipo]}</Insignia>
                    {b.motivo && <span className={p.secundario}>{b.motivo}</span>}
                  </td>
                  <td>{b.odontologo ?? 'Toda la clínica'}</td>
                  <td>{cuandoDelBloqueo(b)}</td>
                  <td>{horasDelBloqueo(b)}</td>
                  <td>
                    <div className={p.celdaAcciones}>{acciones(b)}</div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {accion?.tipo === 'crear' && <DialogoBloqueo alCerrar={cerrar} alTerminar={terminar} />}
      {accion?.tipo === 'editar' && (
        <DialogoBloqueo bloqueo={accion.bloqueo} alCerrar={cerrar} alTerminar={terminar} />
      )}
      {accion?.tipo === 'quitar' && (
        <DialogoConfirmacion
          titulo="¿Quitar este bloqueo?"
          boton="Quitar bloqueo"
          peligro
          accion={() => quitarBloqueo(accion.bloqueo.id)}
          alCerrar={cerrar}
          alTerminar={() =>
            terminar({ mensaje: 'Bloqueo quitado: ese tiempo vuelve a estar libre para dar citas.' })
          }
        >
          <p>
            <strong>
              {BLOQUEOS[accion.bloqueo.tipo]}
              {accion.bloqueo.motivo ? `: ${accion.bloqueo.motivo}` : ''}
            </strong>
            <span className={p.secundario}>
              {accion.bloqueo.odontologo ?? 'Toda la clínica'} · {cuandoDelBloqueo(accion.bloqueo)} ·{' '}
              {horasDelBloqueo(accion.bloqueo)}
            </span>
          </p>
          <p>
            Ese tiempo vuelve a quedar libre al momento para dar citas, también en la web. El bloqueo queda
            guardado en la auditoría.
          </p>
        </DialogoConfirmacion>
      )}
    </div>
  );
}
