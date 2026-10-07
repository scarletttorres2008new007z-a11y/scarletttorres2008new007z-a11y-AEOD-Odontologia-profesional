import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { ApiError, erroresDeCampos, mensajeDeError } from '../../shared/api/errores';
import type { Cita, EstadoCita } from '../../shared/api/tipos';
import { conMayuscula, formatearDia } from '../../shared/fechas';
import { formatearFechaHora, formatearTelefono } from '../../shared/formato';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { AreaDeTexto } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { Insignia } from '../../shared/ui/Insignia';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { cambiarEstadoDeCita, cambiarNotas, obtenerCita } from './api';
import { cambioHecho, ESTADOS, nombreDeCita, ORIGENES, textoDelCambio, useRefrescarCitas } from './comun';
import { DialogoCancelarCita } from './DialogoCancelarCita';
import { DialogoReprogramar } from './DialogoReprogramar';
import { DialogoVincular } from './DialogoVincular';
import css from './Agenda.module.css';

interface Props {
  id: number;
  /** Lo último que se ha hecho con ella (por ejemplo, al abrirla justo después de moverla). */
  aviso?: string;
  alCerrar: () => void;
  /** Para saltar a otra cita: la anterior o la nueva de una reprogramación. */
  alAbrir: (id: number) => void;
  /** Tras moverla, con la cita nueva. */
  alReprogramar: (nueva: Cita, mensaje: string) => void;
}

type Modo = 'detalle' | 'cancelar' | 'reprogramar' | 'vincular';

/** Una cita con todos sus datos y las acciones que la API permite en su estado y con los permisos del usuario. */
export function CitaAbierta({ id, aviso = '', alCerrar, alAbrir, alReprogramar }: Props) {
  const { tienePermiso } = useAuth();
  const queryClient = useQueryClient();
  const refrescar = useRefrescarCitas();
  const [modo, setModo] = useState<Modo>('detalle');
  const [hecho, setHecho] = useState(aviso);
  const {
    data: cita,
    error,
    isPending,
  } = useQuery({ queryKey: ['cita', id], queryFn: ({ signal }) => obtenerCita(id, signal) });

  const actualizar = (nueva: Cita, mensaje: string) => {
    queryClient.setQueryData(['cita', nueva.id], nueva);
    setModo('detalle');
    setHecho(mensaje);
    void refrescar();
  };

  const estado = useMutation({
    mutationFn: (nuevo: EstadoCita) => cambiarEstadoDeCita(id, nuevo),
    onMutate: () => setHecho(''),
    onSuccess: (nueva) => actualizar(nueva, cambioHecho(nueva.estado)),
  });

  if (cita && modo === 'cancelar') {
    return <DialogoCancelarCita cita={cita} alCerrar={() => setModo('detalle')} alTerminar={actualizar} />;
  }
  if (cita && modo === 'reprogramar') {
    return <DialogoReprogramar cita={cita} alCerrar={() => setModo('detalle')} alTerminar={alReprogramar} />;
  }
  if (cita && modo === 'vincular') {
    return <DialogoVincular cita={cita} alCerrar={() => setModo('detalle')} alTerminar={actualizar} />;
  }

  const puedeMover = cita?.modificable && tienePermiso(PERMISOS.CITAS_REPROGRAMAR);
  const puedeCancelar = cita?.modificable && tienePermiso(PERMISOS.CITAS_CANCELAR);
  const noExiste = error instanceof ApiError && error.status === 404;

  return (
    <Dialogo
      titulo={cita ? `Cita de ${nombreDeCita(cita)}` : 'Cita'}
      alCerrar={alCerrar}
      pie={
        <>
          {puedeCancelar && (
            <Boton variante="secundario" onClick={() => setModo('cancelar')}>
              Cancelar la cita
            </Boton>
          )}
          {puedeMover && (
            <Boton variante="secundario" onClick={() => setModo('reprogramar')}>
              Mover a otro día u hora
            </Boton>
          )}
          <Boton
            variante={!cita || puedeCancelar || puedeMover ? 'secundario' : 'primario'}
            onClick={alCerrar}
          >
            Cerrar
          </Boton>
        </>
      }
    >
      {noExiste ? (
        <Aviso tipo="error">Esta cita no existe o no es de tu agenda.</Aviso>
      ) : error ? (
        <Aviso tipo="error">{mensajeDeError(error)}</Aviso>
      ) : isPending ? (
        <Cargando texto="Cargando la cita…" />
      ) : (
        <>
          {hecho && <Aviso tipo="exito">{hecho}</Aviso>}
          <div className={css.estadoCita}>
            <Insignia tono={ESTADOS[cita.estado].tono}>{ESTADOS[cita.estado].texto}</Insignia>
            {tienePermiso(PERMISOS.CITAS_CAMBIAR_ESTADO) && cita.estados_siguientes.length > 0 && (
              <div className={css.accionesEstado}>
                {cita.estados_siguientes.map((siguiente, posicion) => (
                  <Boton
                    key={siguiente}
                    pequeno
                    variante={posicion === 0 ? 'primario' : 'secundario'}
                    cargando={estado.isPending && estado.variables === siguiente}
                    disabled={estado.isPending}
                    onClick={() => estado.mutate(siguiente)}
                  >
                    {textoDelCambio(cita.estado, siguiente)}
                  </Boton>
                ))}
              </div>
            )}
          </div>
          {estado.error && <Aviso tipo="error">{mensajeDeError(estado.error)}</Aviso>}

          <dl className={css.datosCita}>
            <Dato nombre="Día">{conMayuscula(formatearDia(cita.fecha, true))}</Dato>
            <Dato nombre="Hora">
              {cita.hora_inicio} a {cita.hora_fin} ({cita.duracion_minutos} min)
            </Dato>
            <Dato nombre="Tratamiento">{cita.tratamiento}</Dato>
            <Dato nombre="Odontólogo">{cita.odontologo}</Dato>
            <Dato nombre="Paciente">
              {cita.paciente ? (
                <>
                  {tienePermiso(PERMISOS.PACIENTES_VER) ? (
                    <Link to={`/pacientes/${cita.paciente.id}`}>{cita.paciente.nombre}</Link>
                  ) : (
                    cita.paciente.nombre
                  )}
                  <span className={css.secundario}>Código {cita.paciente.codigo}</span>
                </>
              ) : (
                <>
                  {cita.contacto.nombre}
                  <span className={css.secundario}>Sin ficha todavía: son los datos que dejó en la web</span>
                </>
              )}
            </Dato>
            <Dato nombre="Teléfono">
              <a href={`tel:${telefonoDe(cita)}`}>{formatearTelefono(telefonoDe(cita))}</a>
            </Dato>
            {(cita.paciente?.email ?? cita.contacto.email) && (
              <Dato nombre="Correo">{cita.paciente?.email ?? cita.contacto.email}</Dato>
            )}
            {cita.contacto.mensaje && <Dato nombre="Mensaje al reservar">{cita.contacto.mensaje}</Dato>}
          </dl>

          {!cita.paciente && tienePermiso(PERMISOS.CITAS_EDITAR) && tienePermiso(PERMISOS.PACIENTES_VER) && (
            <div className={css.vincular}>
              <p>Vincúlala a la ficha del paciente para que quede en su historial.</p>
              <Boton variante="secundario" pequeno onClick={() => setModo('vincular')}>
                Vincular a un paciente
              </Boton>
            </div>
          )}

          <Notas key={cita.notas_internas ?? ''} cita={cita} alGuardar={actualizar} />
          <Historia cita={cita} alAbrir={alAbrir} />
        </>
      )}
    </Dialogo>
  );
}

function telefonoDe(cita: Cita): string {
  return cita.paciente?.telefono ?? cita.contacto.telefono;
}

function Dato({ nombre, children }: { nombre: string; children: ReactNode }) {
  return (
    <div>
      <dt>{nombre}</dt>
      <dd>{children}</dd>
    </div>
  );
}

/** Notas para el personal. Quien puede editar la cita las cambia aquí mismo. */
function Notas({ cita, alGuardar }: { cita: Cita; alGuardar: (cita: Cita, mensaje: string) => void }) {
  const { tienePermiso } = useAuth();
  const idTitulo = useId();
  const [editando, setEditando] = useState(false);
  const [texto, setTexto] = useState(cita.notas_internas ?? '');
  const guardar = useMutation({
    mutationFn: () => cambiarNotas(cita.id, texto),
    onSuccess: (nueva) => {
      setEditando(false);
      alGuardar(nueva, 'Notas guardadas.');
    },
  });
  const puedeEditar = tienePermiso(PERMISOS.CITAS_EDITAR);

  return (
    <section className={css.notas} aria-labelledby={idTitulo}>
      <h3 id={idTitulo}>Notas internas</h3>
      {editando ? (
        <form
          className={css.formNotas}
          onSubmit={(evento) => {
            evento.preventDefault();
            guardar.mutate();
          }}
        >
          <AreaDeTexto
            etiqueta="Notas para el personal"
            maxLength={1000}
            rows={3}
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
            ayuda="El paciente no las ve."
            error={erroresDeCampos(guardar.error).notas_internas}
            autoFocus
          />
          {guardar.error && !erroresDeCampos(guardar.error).notas_internas && (
            <Aviso tipo="error">{mensajeDeError(guardar.error)}</Aviso>
          )}
          <div className={css.botonesNotas}>
            <Boton
              variante="secundario"
              pequeno
              onClick={() => {
                setTexto(cita.notas_internas ?? '');
                setEditando(false);
              }}
            >
              Descartar
            </Boton>
            <Boton type="submit" pequeno cargando={guardar.isPending}>
              Guardar notas
            </Boton>
          </div>
        </form>
      ) : (
        <>
          {cita.notas_internas ? (
            <p className={css.textoNotas}>{cita.notas_internas}</p>
          ) : (
            <p className={css.nota}>Sin notas.</p>
          )}
          {puedeEditar && (
            <Boton variante="texto" pequeno onClick={() => setEditando(true)}>
              {cita.notas_internas ? 'Editar las notas' : 'Añadir notas'}
            </Boton>
          )}
        </>
      )}
    </section>
  );
}

/** De dónde vino la cita y qué ha pasado con ella. */
function Historia({ cita, alAbrir }: { cita: Cita; alAbrir: (id: number) => void }) {
  return (
    <ul className={css.historia}>
      <li>
        {cita.origen === 'SOFTWARE'
          ? `Dada en la clínica${cita.creada_por ? ` por ${cita.creada_por}` : ''}`
          : `Reservada en ${ORIGENES[cita.origen]}`}{' '}
        el {formatearFechaHora(cita.creada_en)}.
      </li>
      {cita.cita_anterior_id && (
        <li>
          Sustituye a una cita que se movió de día u hora.{' '}
          <Boton variante="texto" pequeno onClick={() => alAbrir(cita.cita_anterior_id as number)}>
            Ver la cita anterior
          </Boton>
        </li>
      )}
      {cita.estado === 'CANCELADA' && (
        <li>
          Cancelada {cita.cancelada_por ? `desde ${ORIGENES[cita.cancelada_por]} ` : ''}el{' '}
          {formatearFechaHora(cita.cancelada_en)}.
          {cita.motivo_cancelacion && ` Motivo: ${cita.motivo_cancelacion}`}
        </li>
      )}
      {cita.cita_nueva_id && (
        <li>
          Se movió a otro día u hora.{' '}
          <Boton variante="texto" pequeno onClick={() => alAbrir(cita.cita_nueva_id as number)}>
            Ver la cita nueva
          </Boton>
        </li>
      )}
    </ul>
  );
}
