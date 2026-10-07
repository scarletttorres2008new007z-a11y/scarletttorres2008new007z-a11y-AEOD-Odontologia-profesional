import type { RegistroAuditoria } from '../../shared/api/tipos';
import { formatearFechaHora } from '../../shared/formato';
import { Boton } from '../../shared/ui/Boton';
import { Dialogo } from '../../shared/ui/Dialogo';
import p from '../../shared/ui/pagina.module.css';
import css from './Auditoria.module.css';
import { afectado, autor, nombreDelCampo, ORIGENES, textoDeAccion, textoDelValor } from './etiquetas';

type Valores = Record<string, unknown>;

/** Todo lo que se guardó de una operación, con los valores de antes y de después lado a lado. */
export function DetalleAuditoria({
  registro,
  alCerrar,
}: {
  registro: RegistroAuditoria;
  alCerrar: () => void;
}) {
  const quien = autor(registro);
  const antes = comoObjeto(registro.valor_anterior);
  const despues = comoObjeto(registro.valor_nuevo);
  const claves = [...new Set([...Object.keys(antes ?? {}), ...Object.keys(despues ?? {})])];

  return (
    <Dialogo
      titulo={textoDeAccion(registro)}
      alCerrar={alCerrar}
      pie={
        <Boton variante="secundario" onClick={alCerrar}>
          Cerrar
        </Boton>
      }
    >
      <dl className={css.datos}>
        <div>
          <dt>Fecha y hora</dt>
          <dd>{formatearFechaHora(registro.creado_en, true)}</dd>
        </div>
        <div>
          <dt>Quién</dt>
          <dd>
            {quien.nombre}
            {quien.detalle && <span className={p.secundario}>{quien.detalle}</span>}
          </dd>
        </div>
        <div>
          <dt>Desde</dt>
          <dd>{ORIGENES[registro.origen]}</dd>
        </div>
        <div>
          <dt>Afecta a</dt>
          <dd>{afectado(registro)}</dd>
        </div>
        {registro.ip && (
          <div>
            <dt>Dirección IP</dt>
            <dd>{registro.ip}</dd>
          </div>
        )}
      </dl>

      {claves.length > 0 && (
        <div className={p.tablaCaja}>
          <table className={p.tabla}>
            <caption className="visually-hidden">Datos guardados</caption>
            <thead>
              <tr>
                <th scope="col">Dato</th>
                {antes && <th scope="col">Antes</th>}
                {despues && <th scope="col">{antes ? 'Después' : 'Valor'}</th>}
              </tr>
            </thead>
            <tbody>
              {claves.map((clave) => {
                const cambio =
                  antes &&
                  despues &&
                  textoDelValor(clave, antes[clave]) !== textoDelValor(clave, despues[clave]);
                return (
                  <tr key={clave} className={cambio ? css.cambio : undefined}>
                    <th scope="row">{nombreDelCampo(clave)}</th>
                    {antes && <td>{textoDelValor(clave, antes[clave])}</td>}
                    {despues && <td>{textoDelValor(clave, despues[clave])}</td>}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Dialogo>
  );
}

function comoObjeto(valor: unknown): Valores | null {
  if (valor === null || valor === undefined) return null;
  if (typeof valor === 'object' && !Array.isArray(valor)) return valor as Valores;
  return { valor };
}
