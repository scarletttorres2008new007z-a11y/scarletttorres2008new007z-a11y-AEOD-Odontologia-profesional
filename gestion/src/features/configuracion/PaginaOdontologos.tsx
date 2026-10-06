import { useQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../../shared/api/errores';
import type { OdontologoConfigurado } from '../../shared/api/tipos';
import { useTitulo } from '../../shared/hooks';
import { Aviso, Cargando, Vacio } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { cambiarEstadoDelOdontologo, CLAVES, listarOdontologos } from './api';
import { AvisoDeResultado } from './AvisoDeResultado';
import type { Resultado } from './comun';
import { DialogoConfirmacion } from './DialogoConfirmacion';
import { DialogoOdontologo } from './DialogoOdontologo';

type Accion = { tipo: 'crear' } | { tipo: 'editar' | 'estado'; odontologo: OdontologoConfigurado };

export function PaginaOdontologos() {
  useTitulo('Odontólogos');
  const { tienePermiso } = useAuth();
  const odontologos = useQuery({
    queryKey: CLAVES.odontologos,
    queryFn: ({ signal }) => listarOdontologos(signal),
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

  const alCrear = (o: OdontologoConfigurado) => (
    <>
      Se ha dado de alta a {o.nombre}. Para que tenga huecos en la web y en la agenda, ponle sus turnos en{' '}
      {tienePermiso(PERMISOS.HORARIOS_GESTIONAR) ? <Link to="/horarios">Horarios</Link> : '«Horarios»'}.
    </>
  );

  const acciones = (o: OdontologoConfigurado): ReactNode[] => [
    <Boton key="editar" variante="texto" pequeno onClick={() => abrir({ tipo: 'editar', odontologo: o })}>
      Editar<TextoOculto>a {o.nombre}</TextoOculto>
    </Boton>,
    <Boton key="estado" variante="texto" pequeno onClick={() => abrir({ tipo: 'estado', odontologo: o })}>
      {o.activo ? 'Desactivar' : 'Activar'}
      <TextoOculto>a {o.nombre}</TextoOculto>
    </Boton>,
  ];

  return (
    <div className={p.pagina}>
      <div className={p.cabecera}>
        <div>
          <h1>Odontólogos</h1>
          <p className={p.entradilla}>
            Quién atiende en la clínica. Los activos salen en la web y en la agenda. Con su usuario del
            software, cada uno ve su propia agenda.
          </p>
        </div>
        <div className={p.acciones}>
          <Boton onClick={() => abrir({ tipo: 'crear' })}>Nuevo odontólogo</Boton>
        </div>
      </div>

      {resultado && <AvisoDeResultado resultado={resultado} />}
      {odontologos.error && <Aviso tipo="error">{mensajeDeError(odontologos.error)}</Aviso>}
      {odontologos.isPending && <Cargando texto="Cargando odontólogos…" />}
      {odontologos.data?.length === 0 && <Vacio>Todavía no hay odontólogos.</Vacio>}
      {odontologos.data && odontologos.data.length > 0 && (
        <div className={p.tablaCaja}>
          <table className={p.tabla}>
            <caption className="visually-hidden">Odontólogos de la clínica</caption>
            <thead>
              <tr>
                <th scope="col">Odontólogo</th>
                <th scope="col">Usuario del software</th>
                <th scope="col">Tratamientos que hace</th>
                <th scope="col">Estado</th>
                <th scope="col">
                  <span className="visually-hidden">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {odontologos.data.map((o) => (
                <tr key={o.id}>
                  <td>
                    <strong>{o.nombre}</strong>
                    {o.especialidad && <span className={p.secundario}>{o.especialidad}</span>}
                  </td>
                  <td>
                    {o.usuario ? (
                      <>
                        {o.usuario.nombre}
                        <span className={p.secundario}>{o.usuario.username}</span>
                      </>
                    ) : (
                      <span className={p.secundario}>Sin usuario</span>
                    )}
                  </td>
                  <td>
                    {o.tratamientos.length > 0 ? (
                      o.tratamientos.join(', ')
                    ) : (
                      <span className={p.secundario}>Ninguno</span>
                    )}
                  </td>
                  <td>
                    {o.activo ? <Insignia tono="exito">Activo</Insignia> : <Insignia>Desactivado</Insignia>}
                  </td>
                  <td>
                    <div className={p.celdaAcciones}>{acciones(o)}</div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {accion?.tipo === 'crear' && (
        <DialogoOdontologo alCerrar={cerrar} alTerminar={terminar} mensajeAlCrear={alCrear} />
      )}
      {accion?.tipo === 'editar' && (
        <DialogoOdontologo odontologo={accion.odontologo} alCerrar={cerrar} alTerminar={terminar} />
      )}
      {accion?.tipo === 'estado' && (
        <DialogoEstado odontologo={accion.odontologo} alCerrar={cerrar} alTerminar={terminar} />
      )}
    </div>
  );
}

function DialogoEstado({
  odontologo,
  alCerrar,
  alTerminar,
}: {
  odontologo: OdontologoConfigurado;
  alCerrar: () => void;
  alTerminar: (resultado: Resultado) => void;
}) {
  const { id, nombre, activo } = odontologo;
  if (activo) {
    return (
      <DialogoConfirmacion
        titulo={`¿Desactivar a ${nombre}?`}
        boton="Desactivar"
        peligro
        accion={() => cambiarEstadoDelOdontologo(id, false)}
        alCerrar={alCerrar}
        alTerminar={() => alTerminar({ mensaje: `${nombre} ya no sale en la web ni en la agenda.` })}
      >
        <p>
          Dejará de salir en la web y en la agenda, y no se le podrán dar citas nuevas. Su historial se
          conserva y puedes volver a activarlo cuando quieras.
        </p>
        <p>
          Si tiene citas pendientes o confirmadas, antes hay que moverlas a otro odontólogo o cancelarlas.
        </p>
      </DialogoConfirmacion>
    );
  }
  return (
    <DialogoConfirmacion
      titulo={`¿Activar a ${nombre}?`}
      boton="Activar"
      accion={() => cambiarEstadoDelOdontologo(id, true)}
      alCerrar={alCerrar}
      alTerminar={() => alTerminar({ mensaje: `${nombre} vuelve a salir en la web y en la agenda.` })}
    >
      <p>Volverá a salir en la web y en la agenda, con los turnos que tenía.</p>
    </DialogoConfirmacion>
  );
}
