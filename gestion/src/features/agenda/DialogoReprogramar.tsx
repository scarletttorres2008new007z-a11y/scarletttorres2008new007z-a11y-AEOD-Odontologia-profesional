import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError, erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import type { Cita, Hueco } from '../../shared/api/tipos';
import { hoyEnMadrid, sumarDias } from '../../shared/fechas';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo, Selector } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { reprogramarCita } from './api';
import {
  cuando,
  nombreDeCita,
  quienesLoHacen,
  useOdontologos,
  useRefrescarCitas,
  useTratamientos,
} from './comun';
import { SelectorDeHueco } from './SelectorDeHueco';
import css from './Agenda.module.css';

const CAMPOS = ['odontologo_id', 'fecha', 'hora_inicio'] as const;

interface Props {
  cita: Cita;
  alCerrar: () => void;
  /** Con la cita nueva: la anterior queda como reprogramada. */
  alTerminar: (nueva: Cita, mensaje: string) => void;
}

/** Mover la cita a otro día, hora u odontólogo, entre los huecos libres que da la API. */
export function DialogoReprogramar({ cita, alCerrar, alTerminar }: Props) {
  const queryClient = useQueryClient();
  const refrescar = useRefrescarCitas();
  const hoy = hoyEnMadrid();
  const [fecha, setFecha] = useState(cita.fecha < hoy ? hoy : cita.fecha);
  const [odontologoId, setOdontologoId] = useState(String(cita.odontologo_id));
  const [hueco, setHueco] = useState<Hueco | null>(null);
  const [falta, setFalta] = useState('');

  const tratamientos = useTratamientos();
  const odontologos = useOdontologos();
  const tratamiento = tratamientos.data?.find((t) => t.id === cita.tratamiento_id);
  const posibles = quienesLoHacen(tratamiento, odontologos.data ?? []);

  const guardar = useMutation({
    mutationFn: (elegido: Hueco) =>
      reprogramarCita(cita.id, {
        odontologo_id: elegido.odontologo_id,
        fecha,
        hora_inicio: elegido.hora_inicio,
      }),
    onSuccess: async (nueva) => {
      await refrescar();
      alTerminar(nueva, `Cita movida al ${cuando(nueva)}.`);
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        setHueco(null);
        void queryClient.invalidateQueries({ queryKey: ['huecos'] });
      }
    },
  });

  const enviar = () => {
    setFalta(hueco ? '' : 'Elige uno de los huecos libres.');
    if (hueco) guardar.mutate(hueco);
  };
  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, CAMPOS);

  return (
    <Dialogo
      titulo="Mover la cita"
      ancho="amplio"
      alCerrar={alCerrar}
      alEnviar={enviar}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Volver
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            Mover la cita
          </Boton>
        </>
      }
    >
      <p>
        <strong>{nombreDeCita(cita)}</strong>: {cita.tratamiento} ({cita.duracion_minutos} min). Ahora está el{' '}
        {cuando(cita)}.
      </p>
      <div className={css.columnas}>
        <Campo
          etiqueta="Nuevo día"
          type="date"
          min={hoy}
          value={fecha}
          error={errores.fecha}
          onChange={(evento) => {
            setFecha(evento.target.value);
            setHueco(null);
          }}
        />
        <Selector
          etiqueta="Odontólogo"
          value={odontologoId}
          error={errores.odontologo_id}
          onChange={(evento) => {
            setOdontologoId(evento.target.value);
            setHueco(null);
          }}
        >
          <option value="">Cualquiera que lo haga</option>
          {!posibles.some((o) => o.id === cita.odontologo_id) && (
            <option value={cita.odontologo_id}>{cita.odontologo}</option>
          )}
          {posibles.map((o) => (
            <option key={o.id} value={o.id}>
              {o.nombre}
            </option>
          ))}
        </Selector>
      </div>
      <SelectorDeHueco
        peticion={
          fecha
            ? {
                tratamientoId: cita.tratamiento_id,
                fecha,
                odontologoId: odontologoId ? Number(odontologoId) : null,
                excluirCitaId: cita.id,
              }
            : null
        }
        elegido={hueco}
        alElegir={(elegido) => {
          setHueco(elegido);
          setFalta('');
        }}
        actual={{ odontologoId: cita.odontologo_id, fecha: cita.fecha, horaInicio: cita.hora_inicio }}
        error={falta || errores.hora_inicio}
        alPedirDiaSiguiente={() => {
          setFecha(sumarDias(fecha, 1));
          setHueco(null);
        }}
      />
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
