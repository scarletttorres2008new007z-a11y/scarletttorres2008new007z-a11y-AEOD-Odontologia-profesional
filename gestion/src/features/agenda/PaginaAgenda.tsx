import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { mensajeDeError } from '../../shared/api/errores';
import type { Cita } from '../../shared/api/tipos';
import {
  conMayuscula,
  esFecha,
  formatearDia,
  formatearSemana,
  hoyEnMadrid,
  lunesDe,
  sumarDias,
} from '../../shared/fechas';
import { useTitulo } from '../../shared/hooks';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo, Selector } from '../../shared/ui/Campo';
import { Icono } from '../../shared/ui/Icono';
import p from '../../shared/ui/pagina.module.css';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { obtenerAgenda } from './api';
import { CitaAbierta } from './CitaAbierta';
import { ESTADOS, ESTADOS_EN_AGENDA, useOdontologos } from './comun';
import { DialogoNuevaCita } from './DialogoNuevaCita';
import { VistaDia } from './VistaDia';
import { VistaSemana } from './VistaSemana';
import css from './Agenda.module.css';

type Vista = 'dia' | 'semana';

/** Lo que se acaba de hacer, con un enlace a la cita. */
interface Hecho {
  mensaje: string;
  cita: Cita;
}

function numero(texto: string | null): number | null {
  const valor = Number(texto);
  return Number.isInteger(valor) && valor > 0 ? valor : null;
}

/**
 * Agenda de la clínica. El día, la vista, el odontólogo y la cita abierta van en la dirección
 * (?fecha=2026-10-12&vista=semana&odontologo=3&cita=45), así que se pueden recargar y enlazar.
 */
export function PaginaAgenda() {
  useTitulo('Agenda');
  const { tienePermiso } = useAuth();
  const [parametros, setParametros] = useSearchParams();
  const [nuevaAbierta, setNuevaAbierta] = useState(false);
  const [hecho, setHecho] = useState<Hecho | null>(null);
  const [avisoCita, setAvisoCita] = useState('');
  const hoy = hoyEnMadrid();

  const fecha = esFecha(parametros.get('fecha')) ? (parametros.get('fecha') as string) : hoy;
  const vista: Vista = parametros.get('vista') === 'semana' ? 'semana' : 'dia';
  const odontologoId = numero(parametros.get('odontologo'));
  const citaId = numero(parametros.get('cita'));
  const desde = vista === 'semana' ? lunesDe(fecha) : fecha;
  const dias = vista === 'semana' ? 7 : 1;

  const {
    data: agenda,
    error,
    isPending,
    isPlaceholderData,
  } = useQuery({
    queryKey: ['agenda', desde, dias, odontologoId],
    queryFn: ({ signal }) => obtenerAgenda(desde, dias, odontologoId, signal),
    // Al pasar de un día a otro se ve el anterior mientras carga; al cambiar de día a semana, no (no encajaría)
    placeholderData: (anterior, consulta) => (consulta?.queryKey[2] === dias ? anterior : undefined),
    // Las reservas de la web aparecen solas, sin recargar
    refetchInterval: 60_000,
  });
  const odontologos = useOdontologos();

  const cambiar = (cambios: Record<string, string | null>) =>
    setParametros(
      (antes) => {
        const nuevos = new URLSearchParams(antes);
        for (const [clave, valor] of Object.entries(cambios)) {
          if (valor) nuevos.set(clave, valor);
          else nuevos.delete(clave);
        }
        return nuevos;
      },
      { replace: true },
    );

  const direccion = (cambios: Record<string, string | null>) => {
    const nuevos = new URLSearchParams(parametros);
    for (const [clave, valor] of Object.entries(cambios)) {
      if (valor) nuevos.set(clave, valor);
      else nuevos.delete(clave);
    }
    return `?${nuevos.toString()}`;
  };
  const enlace = (cita: { id: number }) => direccion({ cita: String(cita.id) });
  const enlaceDelDia = (dia: string) => direccion({ fecha: dia, vista: null, cita: null });

  const paso = vista === 'semana' ? 7 : 1;
  const unidad = vista === 'semana' ? 'Semana' : 'Día';
  const titulo =
    vista === 'semana' ? `Semana ${formatearSemana(desde)}` : conMayuscula(formatearDia(fecha, true));
  const puedeDarCita = tienePermiso(PERMISOS.CITAS_CREAR) && tienePermiso(PERMISOS.PACIENTES_VER);
  const soloSuAgenda = agenda?.solo_su_agenda ?? !tienePermiso(PERMISOS.CITAS_VER_TODAS);

  const terminarNueva = (cita: Cita, mensaje: string) => {
    setNuevaAbierta(false);
    setHecho({ cita, mensaje });
    cambiar({ fecha: cita.fecha });
  };

  return (
    <div className={p.pagina}>
      <div className={p.cabecera}>
        <div>
          <h1>Agenda</h1>
          <p className={p.entradilla}>
            {soloSuAgenda
              ? 'Tus citas, las de la web y las que da la clínica.'
              : 'Las citas de todos los odontólogos: las de la web y las que da la clínica.'}
          </p>
        </div>
        {puedeDarCita && (
          <div className={p.acciones}>
            <Boton
              onClick={() => {
                setHecho(null);
                setNuevaAbierta(true);
              }}
            >
              Nueva cita
            </Boton>
          </div>
        )}
      </div>

      <div className={css.barra}>
        <div className={css.navegacion}>
          <Boton
            variante="secundario"
            pequeno
            onClick={() => cambiar({ fecha: null })}
            disabled={fecha === hoy}
          >
            Hoy
          </Boton>
          <Boton
            variante="secundario"
            pequeno
            aria-label={`${unidad} anterior`}
            onClick={() => cambiar({ fecha: sumarDias(fecha, -paso) })}
          >
            <Icono nombre="anterior" tamano={16} />
          </Boton>
          <Boton
            variante="secundario"
            pequeno
            aria-label={`${unidad} siguiente`}
            onClick={() => cambiar({ fecha: sumarDias(fecha, paso) })}
          >
            <Icono nombre="siguiente" tamano={16} />
          </Boton>
          <h2 className={css.titulo} aria-live="polite">
            {titulo}
          </h2>
        </div>
        <div className={css.controles}>
          <div className={css.vistas} role="group" aria-label="Vista">
            <button type="button" aria-pressed={vista === 'dia'} onClick={() => cambiar({ vista: null })}>
              Día
            </button>
            <button
              type="button"
              aria-pressed={vista === 'semana'}
              onClick={() => cambiar({ vista: 'semana' })}
            >
              Semana
            </button>
          </div>
          <Campo
            etiqueta="Ir al día"
            type="date"
            value={fecha}
            onChange={(evento) => {
              if (esFecha(evento.target.value)) cambiar({ fecha: evento.target.value });
            }}
          />
          {!soloSuAgenda && (
            <Selector
              etiqueta="Odontólogo"
              value={odontologoId ?? ''}
              onChange={(evento) => cambiar({ odontologo: evento.target.value || null })}
            >
              <option value="">Todos</option>
              {odontologos.data?.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.nombre}
                </option>
              ))}
            </Selector>
          )}
        </div>
      </div>

      <ul className={css.leyenda} aria-label="Colores de la agenda">
        {ESTADOS_EN_AGENDA.map((estado) => (
          <li key={estado}>
            <span className={css.muestra} data-estado={estado} aria-hidden="true" />
            {ESTADOS[estado].texto}
          </li>
        ))}
      </ul>

      {hecho && (
        <Aviso tipo="exito">
          {hecho.mensaje} <Link to={enlace(hecho.cita)}>Ver la cita</Link>
        </Aviso>
      )}
      {error && <Aviso tipo="error">{mensajeDeError(error)}</Aviso>}
      {isPending ? (
        <Cargando texto="Cargando la agenda…" />
      ) : agenda ? (
        <div aria-busy={isPlaceholderData} className={css.contenido}>
          {vista === 'dia' ? (
            <VistaDia agenda={agenda} enlace={enlace} />
          ) : (
            <VistaSemana agenda={agenda} hoy={hoy} enlace={enlace} enlaceDelDia={enlaceDelDia} />
          )}
        </div>
      ) : null}

      {nuevaAbierta && (
        <DialogoNuevaCita
          fecha={fecha}
          odontologoId={odontologoId}
          alCerrar={() => setNuevaAbierta(false)}
          alTerminar={terminarNueva}
        />
      )}
      {citaId && (
        <CitaAbierta
          key={citaId}
          id={citaId}
          aviso={avisoCita}
          alCerrar={() => {
            setAvisoCita('');
            cambiar({ cita: null });
          }}
          alAbrir={(id) => {
            setAvisoCita('');
            cambiar({ cita: String(id) });
          }}
          alReprogramar={(nueva, mensaje) => {
            setAvisoCita(mensaje);
            cambiar({ cita: String(nueva.id), fecha: nueva.fecha });
          }}
        />
      )}
    </div>
  );
}
