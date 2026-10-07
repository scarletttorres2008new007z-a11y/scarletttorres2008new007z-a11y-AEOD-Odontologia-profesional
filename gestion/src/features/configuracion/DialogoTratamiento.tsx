import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeDeError, mensajeGeneral } from '../../shared/api/errores';
import type { DatosTratamiento, TratamientoConfigurado } from '../../shared/api/tipos';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { AreaDeTexto, Campo, Casilla } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import ui from '../../shared/ui/ui.module.css';
import { useOdontologos } from '../agenda/comun';
import { crearTratamiento, editarTratamiento } from './api';
import { useRefrescarConfiguracion, type PropsDialogo } from './comun';
import css from './Configuracion.module.css';

const CAMPOS = [
  'nombre',
  'descripcion_corta',
  'precio_desde',
  'duracion_aproximada',
  'duracion_minutos',
  'odontologo_ids',
] as const;

/**
 * Alta de un tratamiento (sin `tratamiento`) o cambio de sus datos. La duración la guarda la API y es la que usa
 * la agenda para calcular los huecos: aquí no se decide cuánto dura una cita.
 */
export function DialogoTratamiento({
  tratamiento,
  alCerrar,
  alTerminar,
}: PropsDialogo & { tratamiento?: TratamientoConfigurado }) {
  const refrescar = useRefrescarConfiguracion();
  const odontologos = useOdontologos();
  const crear = tratamiento === undefined;

  const [nombre, setNombre] = useState(tratamiento?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(tratamiento?.descripcion_corta ?? '');
  const [duracion, setDuracion] = useState(String(tratamiento?.duracion_minutos ?? ''));
  const [duracionEnLaWeb, setDuracionEnLaWeb] = useState(tratamiento?.duracion_aproximada ?? '');
  const [precio, setPrecio] = useState(
    tratamiento?.precio_desde === undefined ? '' : String(tratamiento.precio_desde),
  );
  const [soloAlgunos, setSoloAlgunos] = useState((tratamiento?.odontologos.length ?? 0) > 0);
  const [elegidos, setElegidos] = useState<number[]>(tratamiento?.odontologos.map((o) => o.id) ?? []);
  const [sinElegir, setSinElegir] = useState(false);

  const guardar = useMutation({
    mutationFn: () => {
      // Sin duración se envía vacía: es la API la que responde que hace falta
      const datos = {
        nombre: nombre.trim(),
        descripcion_corta: descripcion.trim() || undefined,
        duracion_minutos: duracion === '' ? undefined : Number(duracion),
        duracion_aproximada: duracionEnLaWeb.trim() || undefined,
        precio_desde: precio.trim() === '' ? undefined : Number(precio),
        odontologo_ids: soloAlgunos ? elegidos : [],
      } as DatosTratamiento;
      return tratamiento ? editarTratamiento(tratamiento.id, datos) : crearTratamiento(datos);
    },
    onSuccess: async (guardado) => {
      await refrescar();
      if (crear) {
        alTerminar({
          mensaje: `Se ha añadido «${guardado.nombre}». Ya sale en la web y se le pueden dar citas.`,
        });
        return;
      }
      const cambiaLaDuracion = guardado.duracion_minutos !== tratamiento.duracion_minutos;
      alTerminar({
        mensaje: cambiaLaDuracion
          ? `Se han guardado los cambios de «${guardado.nombre}». Las citas nuevas ocuparán ${guardado.duracion_minutos} minutos; las que ya estaban dadas no cambian.`
          : `Se han guardado los cambios de «${guardado.nombre}».`,
      });
    },
  });

  const enviar = () => {
    // «Solo algunos» sin nadie marcado se guardaría como «cualquiera»: mejor pedir que se elija
    if (soloAlgunos && elegidos.length === 0) {
      setSinElegir(true);
      return;
    }
    guardar.mutate();
  };

  const errores: Record<string, string> = {
    ...erroresDeCampos(guardar.error),
    ...(sinElegir ? { odontologo_ids: 'Marca al menos un odontólogo, o elige «Cualquier odontólogo».' } : {}),
  };
  const general = mensajeGeneral(guardar.error, CAMPOS);

  // Los odontólogos activos y, si los tenía asignados, los que ya están desactivados
  const desactivados = (tratamiento?.odontologos ?? []).filter((o) => !o.activo);
  const opciones = [
    ...(odontologos.data ?? []).map((o) => ({ id: o.id, nombre: o.nombre, detalle: o.especialidad })),
    ...desactivados.map((o) => ({ id: o.id, nombre: o.nombre, detalle: 'Desactivado: no tiene huecos' })),
  ];
  const marcar = (id: number, marcado: boolean) => {
    setSinElegir(false);
    setElegidos((antes) => (marcado ? [...antes, id] : antes.filter((otro) => otro !== id)));
  };

  return (
    <Dialogo
      titulo={crear ? 'Nuevo tratamiento' : `Editar «${tratamiento.nombre}»`}
      ancho="amplio"
      alCerrar={alCerrar}
      alEnviar={enviar}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            {crear ? 'Añadir tratamiento' : 'Guardar cambios'}
          </Boton>
        </>
      }
    >
      <Campo
        etiqueta="Nombre"
        required
        autoComplete="off"
        maxLength={120}
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
        error={errores.nombre}
      />
      <AreaDeTexto
        etiqueta="Descripción corta"
        rows={2}
        maxLength={255}
        value={descripcion}
        onChange={(e) => setDescripcion(e.target.value)}
        ayuda="Una frase para la web."
        error={errores.descripcion_corta}
      />
      <div className={css.columnas}>
        <Campo
          etiqueta="Duración en la agenda (minutos)"
          required
          type="number"
          inputMode="numeric"
          min={15}
          max={480}
          step={15}
          value={duracion}
          onChange={(e) => setDuracion(e.target.value)}
          ayuda="Lo que ocupa cada cita, de 15 en 15 minutos. Las citas ya dadas no cambian."
          error={errores.duracion_minutos}
        />
        <Campo
          etiqueta="Duración orientativa"
          autoComplete="off"
          maxLength={80}
          value={duracionEnLaWeb}
          onChange={(e) => setDuracionEnLaWeb(e.target.value)}
          ayuda="Opcional, por ejemplo «30–45 min» o «2 o 3 sesiones». Las citas usan la duración en minutos."
          error={errores.duracion_aproximada}
        />
        <Campo
          etiqueta="Precio desde (€)"
          type="number"
          inputMode="decimal"
          min={0}
          step={0.01}
          value={precio}
          onChange={(e) => setPrecio(e.target.value)}
          ayuda="Orientativo. Con 0 o vacío, la web lo muestra como «Gratuita»."
          error={errores.precio_desde}
        />
      </div>
      <fieldset className={ui.grupo}>
        <legend>Quién lo hace</legend>
        <Casilla
          type="radio"
          name="quien"
          etiqueta="Cualquier odontólogo"
          checked={!soloAlgunos}
          onChange={() => {
            setSinElegir(false);
            setSoloAlgunos(false);
          }}
        />
        <Casilla
          type="radio"
          name="quien"
          etiqueta="Solo algunos"
          checked={soloAlgunos}
          onChange={() => setSoloAlgunos(true)}
        />
        {soloAlgunos &&
          (odontologos.error ? (
            <Aviso tipo="error">{mensajeDeError(odontologos.error)}</Aviso>
          ) : odontologos.isPending ? (
            <Cargando texto="Cargando odontólogos…" />
          ) : (
            <div className={css.opciones}>
              {opciones.map((o) => (
                <Casilla
                  key={o.id}
                  etiqueta={o.nombre}
                  detalle={o.detalle}
                  checked={elegidos.includes(o.id)}
                  onChange={(e) => marcar(o.id, e.target.checked)}
                />
              ))}
            </div>
          ))}
        {errores.odontologo_ids && <p className={ui.error}>{errores.odontologo_ids}</p>}
      </fieldset>
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
