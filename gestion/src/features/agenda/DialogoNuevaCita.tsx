import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError, erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import type { Cita, Hueco, Paciente } from '../../shared/api/tipos';
import { hoyEnMadrid, sumarDias } from '../../shared/fechas';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { AreaDeTexto, Campo, Selector } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { nombreCompleto } from '../pacientes/comun';
import { DialogoPaciente } from '../pacientes/DialogoPaciente';
import { darCita } from './api';
import { cuando, quienesLoHacen, useOdontologos, useRefrescarCitas, useTratamientos } from './comun';
import { ElegirPaciente } from './ElegirPaciente';
import { SelectorDeHueco } from './SelectorDeHueco';
import css from './Agenda.module.css';

const CAMPOS = ['tratamiento_id', 'odontologo_id', 'fecha', 'hora_inicio', 'notas_internas'] as const;

interface Props {
  /** Desde la ficha del paciente ya viene elegido; desde la agenda se busca aquí. */
  paciente?: Paciente;
  fecha: string;
  odontologoId?: number | null;
  alCerrar: () => void;
  alTerminar: (cita: Cita, mensaje: string) => void;
}

/** Dar cita a un paciente con ficha, en uno de los huecos libres que calcula la API. */
export function DialogoNuevaCita({
  paciente: fijo,
  fecha: fechaInicial,
  odontologoId: odontologoInicial,
  alCerrar,
  alTerminar,
}: Props) {
  const { tienePermiso } = useAuth();
  const queryClient = useQueryClient();
  const refrescar = useRefrescarCitas();
  const hoy = hoyEnMadrid();
  const [paciente, setPaciente] = useState<Paciente | null>(fijo ?? null);
  const [altaAbierta, setAltaAbierta] = useState(false);
  const [tratamientoId, setTratamientoId] = useState('');
  const [fecha, setFecha] = useState(fechaInicial < hoy ? hoy : fechaInicial);
  const [odontologoId, setOdontologoId] = useState(odontologoInicial ? String(odontologoInicial) : '');
  const [hueco, setHueco] = useState<Hueco | null>(null);
  const [notas, setNotas] = useState('');
  const [faltan, setFaltan] = useState<Record<string, string>>({});

  const tratamientos = useTratamientos();
  const odontologos = useOdontologos();
  const tratamiento = tratamientos.data?.find((t) => String(t.id) === tratamientoId);
  const posibles = quienesLoHacen(tratamiento, odontologos.data ?? []);

  const guardar = useMutation({
    mutationFn: ({ elegido, quien }: { elegido: Hueco; quien: Paciente }) =>
      darCita(quien.id, {
        tratamiento_id: Number(tratamientoId),
        odontologo_id: elegido.odontologo_id,
        fecha,
        hora_inicio: elegido.hora_inicio,
        notas_internas: notas.trim() || undefined,
      }),
    onSuccess: async (cita, { quien }) => {
      await refrescar();
      alTerminar(cita, `Cita dada a ${nombreCompleto(quien)} el ${cuando(cita)}.`);
    },
    onError: (error) => {
      // Otra persona ha cogido ese hueco mientras tanto: se vuelven a pedir los libres
      if (error instanceof ApiError && error.status === 409) {
        setHueco(null);
        void queryClient.invalidateQueries({ queryKey: ['huecos'] });
      }
    },
  });

  if (altaAbierta) {
    return (
      <DialogoPaciente
        alCerrar={() => setAltaAbierta(false)}
        alTerminar={(nuevo) => {
          setPaciente(nuevo);
          setAltaAbierta(false);
        }}
      />
    );
  }

  const enviar = () => {
    const nuevas: Record<string, string> = {};
    if (!paciente) nuevas.paciente = 'Elige el paciente o dalo de alta.';
    if (!tratamientoId) nuevas.tratamiento_id = 'Elige el tratamiento.';
    if (!hueco) nuevas.hora_inicio = 'Elige uno de los huecos libres.';
    setFaltan(nuevas);
    if (paciente && hueco && tratamientoId) guardar.mutate({ elegido: hueco, quien: paciente });
  };

  const errores = { ...erroresDeCampos(guardar.error), ...faltan };
  const general = mensajeGeneral(guardar.error, CAMPOS);
  const quitarFalta = (campo: string) =>
    setFaltan((antes) => {
      const resto = { ...antes };
      delete resto[campo];
      return resto;
    });

  return (
    <Dialogo
      titulo={fijo ? `Dar cita a ${nombreCompleto(fijo)}` : 'Nueva cita'}
      ancho="amplio"
      alCerrar={alCerrar}
      alEnviar={enviar}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            Dar cita
          </Boton>
        </>
      }
    >
      <div className={css.formulario}>
        {!fijo && (
          <fieldset className={css.bloque}>
            <legend>Paciente</legend>
            <ElegirPaciente
              elegido={paciente}
              alElegir={(elegido) => {
                setPaciente(elegido);
                quitarFalta('paciente');
              }}
              error={errores.paciente}
            />
            {!paciente && tienePermiso(PERMISOS.PACIENTES_CREAR) && (
              <p className={css.nota}>
                ¿Es su primera vez?{' '}
                <Boton variante="texto" pequeno onClick={() => setAltaAbierta(true)}>
                  Dar de alta un paciente nuevo
                </Boton>
              </p>
            )}
          </fieldset>
        )}

        <fieldset className={css.bloque}>
          <legend>Cita</legend>
          <div className={css.columnas}>
            <Selector
              etiqueta="Tratamiento"
              value={tratamientoId}
              error={errores.tratamiento_id}
              onChange={(evento) => {
                const nuevo = tratamientos.data?.find((t) => String(t.id) === evento.target.value);
                setTratamientoId(evento.target.value);
                setHueco(null);
                quitarFalta('tratamiento_id');
                // Si el odontólogo elegido no lo hace, se vuelve a «cualquiera»
                if (
                  odontologoId &&
                  !quienesLoHacen(nuevo, odontologos.data ?? []).some((o) => String(o.id) === odontologoId)
                ) {
                  setOdontologoId('');
                }
              }}
            >
              <option value="">{tratamientos.isPending ? 'Cargando…' : 'Elige el tratamiento'}</option>
              {tratamientos.data?.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nombre}
                  {t.duracion_minutos ? ` · ${t.duracion_minutos} min` : ''}
                </option>
              ))}
            </Selector>
            <Campo
              etiqueta="Día"
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
              {posibles.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.nombre}
                </option>
              ))}
            </Selector>
          </div>
          <SelectorDeHueco
            peticion={
              tratamientoId && fecha
                ? {
                    tratamientoId: Number(tratamientoId),
                    fecha,
                    odontologoId: odontologoId ? Number(odontologoId) : null,
                  }
                : null
            }
            elegido={hueco}
            alElegir={(elegido) => {
              setHueco(elegido);
              quitarFalta('hora_inicio');
            }}
            error={errores.hora_inicio}
            alPedirDiaSiguiente={() => {
              setFecha(sumarDias(fecha, 1));
              setHueco(null);
            }}
          />
        </fieldset>

        <AreaDeTexto
          etiqueta="Notas internas"
          maxLength={1000}
          rows={2}
          value={notas}
          error={errores.notas_internas}
          onChange={(evento) => setNotas(evento.target.value)}
          ayuda="Opcional. Solo las ve el personal de la clínica."
        />
      </div>
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
