import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeDeError, mensajeGeneral } from '../../shared/api/errores';
import type { Bloqueo, DatosBloqueo, TipoDeBloqueo } from '../../shared/api/tipos';
import { conMayuscula } from '../../shared/fechas';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo, Casilla, Selector } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import ui from '../../shared/ui/ui.module.css';
import { BLOQUEOS, useOdontologos } from '../agenda/comun';
import { crearBloqueo, editarBloqueo } from './api';
import { DIAS, TIPOS_DE_BLOQUEO, useRefrescarConfiguracion, type PropsDialogo } from './comun';
import css from './Configuracion.module.css';

const CAMPOS = [
  'tipo',
  'motivo',
  'odontologo_id',
  'fecha_inicio',
  'fecha_fin',
  'dia_semana',
  'hora_inicio',
  'hora_fin',
] as const;

/**
 * Nuevo bloqueo o cambio de uno: unos días concretos (un festivo, unas vacaciones) o un día de cada semana (una
 * reunión fija), el día entero o unas horas. La API valida las fechas y las horas y dice qué citas quedan dentro.
 */
export function DialogoBloqueo({ bloqueo, alCerrar, alTerminar }: PropsDialogo & { bloqueo?: Bloqueo }) {
  const refrescar = useRefrescarConfiguracion();
  const odontologos = useOdontologos();
  const crear = bloqueo === undefined;

  const [tipo, setTipo] = useState<TipoDeBloqueo | ''>(bloqueo?.tipo ?? '');
  const [para, setPara] = useState(bloqueo?.odontologo_id ? String(bloqueo.odontologo_id) : '');
  const [cadaSemana, setCadaSemana] = useState(bloqueo?.dia_semana !== undefined);
  const [diaSemana, setDiaSemana] = useState(bloqueo?.dia_semana ? String(bloqueo.dia_semana) : '');
  const [fechaInicio, setFechaInicio] = useState(bloqueo?.fecha_inicio ?? '');
  const [fechaFin, setFechaFin] = useState(bloqueo?.fecha_fin ?? '');
  const [todoElDia, setTodoElDia] = useState(!bloqueo?.hora_inicio);
  const [horaInicio, setHoraInicio] = useState(bloqueo?.hora_inicio ?? '');
  const [horaFin, setHoraFin] = useState(bloqueo?.hora_fin ?? '');
  const [motivo, setMotivo] = useState(bloqueo?.motivo ?? '');
  const [faltan, setFaltan] = useState<Record<string, string>>({});

  const guardar = useMutation({
    mutationFn: () => {
      const datos = {
        tipo: tipo || undefined,
        motivo: motivo.trim() || undefined,
        odontologo_id: para ? Number(para) : undefined,
        fecha_inicio: fechaInicio || undefined,
        fecha_fin: fechaFin || undefined,
        dia_semana: cadaSemana ? Number(diaSemana) : undefined,
        hora_inicio: todoElDia ? undefined : horaInicio,
        hora_fin: todoElDia ? undefined : horaFin,
      } as DatosBloqueo;
      return bloqueo ? editarBloqueo(bloqueo.id, datos) : crearBloqueo(datos);
    },
    onSuccess: async (guardado) => {
      await refrescar();
      alTerminar({
        mensaje: crear
          ? 'Bloqueo guardado: la web y la agenda ya no ofrecen ese tiempo.'
          : 'Cambios guardados en el bloqueo. La web y la agenda ya lo aplican.',
        afectadas: { citas: guardado.citas_afectadas, motivo: 'caen dentro de este bloqueo' },
      });
    },
  });

  const enviar = () => {
    // Sin estos datos la API entendería otra cosa (un día suelto, o el día entero): mejor pedirlos
    const pendientes: Record<string, string> = {};
    if (cadaSemana && !diaSemana) pendientes.dia_semana = 'Elige el día de la semana.';
    if (!todoElDia && !horaInicio)
      pendientes.hora_inicio = 'Indica la hora de inicio, o marca «Todo el día».';
    if (!todoElDia && !horaFin) pendientes.hora_fin = 'Indica la hora de fin, o marca «Todo el día».';
    setFaltan(pendientes);
    if (Object.keys(pendientes).length === 0) guardar.mutate();
  };

  const errores: Record<string, string> = { ...erroresDeCampos(guardar.error), ...faltan };
  const general = mensajeGeneral(guardar.error, CAMPOS);

  // Si el bloqueo es de un odontólogo que ya está desactivado, se enseña igualmente
  const activos = odontologos.data ?? [];
  const suyoDesactivado =
    bloqueo?.odontologo_id !== undefined &&
    odontologos.data &&
    !activos.some((o) => o.id === bloqueo.odontologo_id);

  return (
    <Dialogo
      titulo={crear ? 'Nuevo bloqueo' : 'Cambiar el bloqueo'}
      ancho="amplio"
      alCerrar={alCerrar}
      alEnviar={enviar}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            {crear ? 'Bloquear' : 'Guardar cambios'}
          </Boton>
        </>
      }
    >
      <div className={css.columnas}>
        <Selector
          etiqueta="Tipo"
          value={tipo}
          onChange={(e) => setTipo(e.target.value as TipoDeBloqueo | '')}
          error={errores.tipo}
        >
          <option value="">Elige el tipo…</option>
          {TIPOS_DE_BLOQUEO.map((t) => (
            <option key={t} value={t}>
              {BLOQUEOS[t]}
            </option>
          ))}
        </Selector>
        <Selector
          etiqueta="Para"
          value={para}
          onChange={(e) => setPara(e.target.value)}
          error={errores.odontologo_id ?? (odontologos.error ? mensajeDeError(odontologos.error) : undefined)}
        >
          <option value="">Toda la clínica</option>
          {suyoDesactivado && (
            <option
              value={String(bloqueo?.odontologo_id)}
            >{`${bloqueo?.odontologo ?? ''} (desactivado)`}</option>
          )}
          {activos.map((o) => (
            <option key={o.id} value={String(o.id)}>
              {o.nombre}
            </option>
          ))}
        </Selector>
      </div>

      <fieldset className={ui.grupo}>
        <legend>Cuándo</legend>
        <div className={css.enLinea}>
          <Casilla
            type="radio"
            name="cuando"
            etiqueta="Unos días concretos"
            checked={!cadaSemana}
            onChange={() => setCadaSemana(false)}
          />
          <Casilla
            type="radio"
            name="cuando"
            etiqueta="Un día de cada semana"
            checked={cadaSemana}
            onChange={() => setCadaSemana(true)}
          />
        </div>
        {cadaSemana ? (
          <div className={css.columnas}>
            <Selector
              etiqueta="Día de la semana"
              value={diaSemana}
              onChange={(e) => setDiaSemana(e.target.value)}
              error={errores.dia_semana}
            >
              <option value="">Elige el día…</option>
              {DIAS.map((d) => (
                <option key={d.numero} value={String(d.numero)}>
                  {conMayuscula(d.nombre)}
                </option>
              ))}
            </Selector>
            <Campo
              etiqueta="A partir del"
              type="date"
              value={fechaInicio}
              onChange={(e) => setFechaInicio(e.target.value)}
              ayuda="Opcional: vacío, desde ya."
              error={errores.fecha_inicio}
            />
            <Campo
              etiqueta="Hasta el"
              type="date"
              value={fechaFin}
              onChange={(e) => setFechaFin(e.target.value)}
              ayuda="Opcional: vacío, todas las semanas sin fin."
              error={errores.fecha_fin}
            />
          </div>
        ) : (
          <div className={css.columnas}>
            <Campo
              etiqueta="Primer día"
              type="date"
              required
              value={fechaInicio}
              onChange={(e) => setFechaInicio(e.target.value)}
              error={errores.fecha_inicio}
            />
            <Campo
              etiqueta="Último día"
              type="date"
              value={fechaFin}
              onChange={(e) => setFechaFin(e.target.value)}
              ayuda="Déjalo vacío si es un solo día."
              error={errores.fecha_fin}
            />
          </div>
        )}
      </fieldset>

      <fieldset className={ui.grupo}>
        <legend>Horas</legend>
        <div className={css.enLinea}>
          <Casilla
            type="radio"
            name="horas"
            etiqueta="Todo el día"
            checked={todoElDia}
            onChange={() => setTodoElDia(true)}
          />
          <Casilla
            type="radio"
            name="horas"
            etiqueta="Solo unas horas"
            checked={!todoElDia}
            onChange={() => setTodoElDia(false)}
          />
        </div>
        {!todoElDia && (
          <div className={css.columnas}>
            <Campo
              etiqueta="Desde las"
              type="time"
              step={900}
              value={horaInicio}
              onChange={(e) => setHoraInicio(e.target.value)}
              error={errores.hora_inicio}
            />
            <Campo
              etiqueta="Hasta las"
              type="time"
              step={900}
              value={horaFin}
              onChange={(e) => setHoraFin(e.target.value)}
              error={errores.hora_fin}
            />
          </div>
        )}
      </fieldset>

      <Campo
        etiqueta="Motivo"
        autoComplete="off"
        maxLength={150}
        value={motivo}
        onChange={(e) => setMotivo(e.target.value)}
        ayuda="Opcional. Se ve en la agenda, por ejemplo «Fiesta local» o «Curso de implantología»."
        error={errores.motivo}
      />
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
