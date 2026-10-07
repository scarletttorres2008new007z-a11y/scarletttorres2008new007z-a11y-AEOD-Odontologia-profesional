import { useQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { TratamientoConfigurado } from '../../shared/api/tipos';
import { formatearEuros } from '../../shared/formato';
import { useTitulo } from '../../shared/hooks';
import { Aviso, Cargando, Vacio } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import { cambiarEstadoDelTratamiento, CLAVES, listarTratamientos } from './api';
import { AvisoDeResultado } from './AvisoDeResultado';
import type { Resultado } from './comun';
import { DialogoConfirmacion } from './DialogoConfirmacion';
import { DialogoTratamiento } from './DialogoTratamiento';
import css from './Configuracion.module.css';

type Accion = { tipo: 'crear' } | { tipo: 'editar' | 'estado'; tratamiento: TratamientoConfigurado };

export function PaginaTratamientos() {
  useTitulo('Tratamientos');
  const tratamientos = useQuery({
    queryKey: CLAVES.tratamientos,
    queryFn: ({ signal }) => listarTratamientos(signal),
  });
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

  const acciones = (t: TratamientoConfigurado): ReactNode[] => [
    <Boton key="editar" variante="texto" pequeno onClick={() => abrir({ tipo: 'editar', tratamiento: t })}>
      Editar<TextoOculto>{t.nombre}</TextoOculto>
    </Boton>,
    <Boton key="estado" variante="texto" pequeno onClick={() => abrir({ tipo: 'estado', tratamiento: t })}>
      {t.activo ? 'Dejar de ofrecer' : 'Volver a ofrecer'}
      <TextoOculto>{t.nombre}</TextoOculto>
    </Boton>,
  ];

  return (
    <div className={p.pagina}>
      <div className={p.cabecera}>
        <div>
          <h1>Tratamientos</h1>
          <p className={p.entradilla}>
            Lo que ofrece la clínica. La duración decide cuánto ocupa cada cita en la agenda; los que se
            ofrecen salen en la web y se pueden reservar.
          </p>
        </div>
        <div className={p.acciones}>
          <Boton onClick={() => abrir({ tipo: 'crear' })}>Nuevo tratamiento</Boton>
        </div>
      </div>

      {resultado && <AvisoDeResultado resultado={resultado} />}
      {tratamientos.error && <Aviso tipo="error">{mensajeDeError(tratamientos.error)}</Aviso>}
      {tratamientos.isPending && <Cargando texto="Cargando tratamientos…" />}
      {tratamientos.data?.length === 0 && <Vacio>Todavía no hay tratamientos.</Vacio>}
      {tratamientos.data && tratamientos.data.length > 0 && (
        <div className={p.tablaCaja}>
          <table className={p.tabla}>
            <caption className="visually-hidden">Tratamientos de la clínica</caption>
            <thead>
              <tr>
                <th scope="col">Tratamiento</th>
                <th scope="col">Duración</th>
                <th scope="col">Precio</th>
                <th scope="col">Quién lo hace</th>
                <th scope="col">Estado</th>
                <th scope="col">
                  <span className="visually-hidden">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {tratamientos.data.map((t) => (
                <tr key={t.id}>
                  <td>
                    <strong>{t.nombre}</strong>
                    {t.descripcion_corta && <span className={p.secundario}>{t.descripcion_corta}</span>}
                  </td>
                  <td>
                    {t.duracion_minutos ? (
                      `${t.duracion_minutos} min`
                    ) : (
                      <Insignia tono="alerta">Sin duración</Insignia>
                    )}
                    {t.duracion_aproximada && t.duracion_aproximada !== `${t.duracion_minutos} min` && (
                      <span className={p.secundario}>Orientativa: {t.duracion_aproximada}</span>
                    )}
                  </td>
                  <td className={css.precio}>
                    {t.precio_desde ? (
                      `Desde ${formatearEuros(t.precio_desde)}`
                    ) : (
                      <span className={p.secundario}>Gratuito</span>
                    )}
                  </td>
                  <td>
                    <QuienLoHace tratamiento={t} />
                  </td>
                  <td>
                    {t.activo ? (
                      <Insignia tono="exito">Se ofrece</Insignia>
                    ) : (
                      <Insignia>No se ofrece</Insignia>
                    )}
                  </td>
                  <td>
                    <div className={p.celdaAcciones}>{acciones(t)}</div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {accion?.tipo === 'crear' && <DialogoTratamiento alCerrar={cerrar} alTerminar={terminar} />}
      {accion?.tipo === 'editar' && (
        <DialogoTratamiento tratamiento={accion.tratamiento} alCerrar={cerrar} alTerminar={terminar} />
      )}
      {accion?.tipo === 'estado' && (
        <DialogoEstado tratamiento={accion.tratamiento} alCerrar={cerrar} alTerminar={terminar} />
      )}
    </div>
  );
}

/** «Cualquier odontólogo» o sus nombres; avisa si ninguno de los elegidos sigue activo (no tendría huecos). */
function QuienLoHace({ tratamiento }: { tratamiento: TratamientoConfigurado }) {
  const { odontologos } = tratamiento;
  if (odontologos.length === 0) return <>Cualquier odontólogo</>;
  return (
    <>
      {odontologos.map((o) => (o.activo ? o.nombre : `${o.nombre} (desactivado)`)).join(', ')}
      {tratamiento.activo && !odontologos.some((o) => o.activo) && (
        <>
          {' '}
          <Insignia tono="alerta">Nadie lo puede hacer ahora</Insignia>
        </>
      )}
    </>
  );
}

function DialogoEstado({
  tratamiento,
  alCerrar,
  alTerminar,
}: {
  tratamiento: TratamientoConfigurado;
  alCerrar: () => void;
  alTerminar: (resultado: Resultado) => void;
}) {
  const { id, nombre, activo } = tratamiento;
  if (activo) {
    return (
      <DialogoConfirmacion
        titulo={`¿Dejar de ofrecer «${nombre}»?`}
        boton="Dejar de ofrecer"
        peligro
        accion={() => cambiarEstadoDelTratamiento(id, false)}
        alCerrar={alCerrar}
        alTerminar={() =>
          alTerminar({
            mensaje: `«${nombre}» ya no se ofrece: no sale en la web ni se pueden dar citas nuevas.`,
          })
        }
      >
        <p>
          Dejará de salir en la web y no se podrán dar citas nuevas de este tratamiento. Su historial se
          conserva y puedes volver a ofrecerlo cuando quieras.
        </p>
        <p>
          Si tiene citas pendientes o confirmadas, antes tienen que pasar o hay que moverlas o cancelarlas.
        </p>
      </DialogoConfirmacion>
    );
  }
  return (
    <DialogoConfirmacion
      titulo={`¿Volver a ofrecer «${nombre}»?`}
      boton="Volver a ofrecer"
      accion={() => cambiarEstadoDelTratamiento(id, true)}
      alCerrar={alCerrar}
      alTerminar={() => alTerminar({ mensaje: `«${nombre}» vuelve a ofrecerse en la web y en la agenda.` })}
    >
      <p>Volverá a salir en la web y se podrán dar citas de este tratamiento.</p>
    </DialogoConfirmacion>
  );
}
