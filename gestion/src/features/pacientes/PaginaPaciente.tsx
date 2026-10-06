import { useQuery } from '@tanstack/react-query';
import { useId, useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router';
import { ApiError, mensajeDeError } from '../../shared/api/errores';
import type { Paciente } from '../../shared/api/tipos';
import { formatearFecha, formatearFechaHora, formatearTelefono } from '../../shared/formato';
import { useTitulo } from '../../shared/hooks';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Icono } from '../../shared/ui/Icono';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { obtenerPaciente } from './api';
import { documento, nombreCompleto, SEXOS, textoDeEdad } from './comun';
import { DialogoEstadoPaciente } from './DialogoEstadoPaciente';
import { DialogoPaciente } from './DialogoPaciente';
import css from './Pacientes.module.css';

/** Ficha de un paciente: sus datos personales, de contacto y administrativos. */
export function PaginaPaciente() {
  const id = Number(useParams().id);
  const valido = Number.isInteger(id) && id > 0;
  const {
    data: paciente,
    error,
    isPending,
  } = useQuery({
    queryKey: ['paciente', id],
    queryFn: ({ signal }) => obtenerPaciente(id, signal),
    enabled: valido,
  });
  useTitulo(paciente ? nombreCompleto(paciente) : 'Paciente');

  const noExiste = !valido || (error instanceof ApiError && error.status === 404);
  return (
    <div className={p.pagina}>
      <Link to="/pacientes" className={css.volver}>
        <Icono nombre="volver" tamano={16} />
        Pacientes
      </Link>
      {noExiste ? (
        <div>
          <h1>Este paciente no existe</h1>
          <p className={p.entradilla}>
            Puede que el enlace esté mal escrito. Búscalo en la lista de pacientes.
          </p>
        </div>
      ) : error ? (
        <Aviso tipo="error">{mensajeDeError(error)}</Aviso>
      ) : isPending ? (
        <Cargando texto="Cargando el paciente…" />
      ) : (
        <Ficha paciente={paciente} />
      )}
    </div>
  );
}

function Ficha({ paciente }: { paciente: Paciente }) {
  const { tienePermiso } = useAuth();
  const [abierto, setAbierto] = useState<'editar' | 'estado' | null>(null);
  const [hecho, setHecho] = useState('');
  const puedeEditar = tienePermiso(PERMISOS.PACIENTES_EDITAR);
  const nacimiento = paciente.fecha_nacimiento
    ? `${formatearFecha(paciente.fecha_nacimiento)} (${textoDeEdad(paciente.edad)})`
    : '';

  const abrir = (dialogo: 'editar' | 'estado') => {
    setHecho('');
    setAbierto(dialogo);
  };
  const terminar = (_guardado: Paciente, mensaje: string) => {
    setAbierto(null);
    setHecho(mensaje);
  };

  return (
    <>
      <div className={p.cabecera}>
        <div>
          <h1>{nombreCompleto(paciente)}</h1>
          <p className={css.titular}>
            <span>Código {paciente.codigo}</span>
            {paciente.activo ? <Insignia tono="exito">Activo</Insignia> : <Insignia>De baja</Insignia>}
          </p>
        </div>
        {puedeEditar && (
          <div className={p.acciones}>
            <Boton variante="secundario" onClick={() => abrir('estado')}>
              {paciente.activo ? 'Dar de baja' : 'Reactivar'}
            </Boton>
            <Boton onClick={() => abrir('editar')}>Editar datos</Boton>
          </div>
        )}
      </div>

      {hecho && <Aviso tipo="exito">{hecho}</Aviso>}

      <div className={css.secciones}>
        <Seccion titulo="Datos personales">
          <Dato nombre="Documento">{documento(paciente)}</Dato>
          <Dato nombre="Fecha de nacimiento">{nacimiento}</Dato>
          <Dato nombre="Sexo">{paciente.sexo ? SEXOS[paciente.sexo] : ''}</Dato>
        </Seccion>

        <Seccion titulo="Contacto">
          <Dato nombre="Teléfono">
            <a href={`tel:${paciente.telefono}`}>{formatearTelefono(paciente.telefono)}</a>
          </Dato>
          <Dato nombre="Correo">
            {paciente.email && <a href={`mailto:${paciente.email}`}>{paciente.email}</a>}
          </Dato>
          <Dato nombre="Dirección">{paciente.direccion}</Dato>
        </Seccion>

        <Seccion titulo="Contacto de emergencia">
          <Dato nombre="Nombre">{paciente.contacto_emergencia_nombre}</Dato>
          <Dato nombre="Teléfono">
            {paciente.contacto_emergencia_telefono && (
              <a href={`tel:${paciente.contacto_emergencia_telefono}`}>
                {formatearTelefono(paciente.contacto_emergencia_telefono)}
              </a>
            )}
          </Dato>
        </Seccion>

        <section className={p.tarjeta} aria-labelledby="titulo-observaciones">
          <h2 id="titulo-observaciones">Observaciones administrativas</h2>
          {paciente.observaciones ? (
            <p className={css.texto}>{paciente.observaciones}</p>
          ) : (
            <p className={`${css.texto} ${css.sinDato}`}>Sin observaciones.</p>
          )}
        </section>
      </div>

      <p className={css.pie}>
        Alta: {formatearFechaHora(paciente.creado_en)} · Última modificación:{' '}
        {formatearFechaHora(paciente.actualizado_en)}
      </p>

      {abierto === 'editar' && (
        <DialogoPaciente paciente={paciente} alCerrar={() => setAbierto(null)} alTerminar={terminar} />
      )}
      {abierto === 'estado' && (
        <DialogoEstadoPaciente paciente={paciente} alCerrar={() => setAbierto(null)} alTerminar={terminar} />
      )}
    </>
  );
}

function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  const idTitulo = useId();
  return (
    <section className={p.tarjeta} aria-labelledby={idTitulo}>
      <h2 id={idTitulo}>{titulo}</h2>
      <dl className={css.datos}>{children}</dl>
    </section>
  );
}

/** Una fila de la ficha. Sin valor, lo dice en lugar de dejar el hueco vacío. */
function Dato({ nombre, children }: { nombre: string; children: ReactNode }) {
  return (
    <div>
      <dt>{nombre}</dt>
      <dd>{children || <span className={css.sinDato}>Sin indicar</span>}</dd>
    </div>
  );
}
