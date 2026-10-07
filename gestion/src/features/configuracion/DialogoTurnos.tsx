import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import type { DiaDeApertura, TurnosDeOdontologo } from '../../shared/api/tipos';
import { conMayuscula, minutosDe } from '../../shared/fechas';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Dialogo } from '../../shared/ui/Dialogo';
import p from '../../shared/ui/pagina.module.css';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import ui from '../../shared/ui/ui.module.css';
import { cambiarTurnos } from './api';
import { DIAS, tramo, useRefrescarConfiguracion, type PropsDialogo } from './comun';
import css from './Configuracion.module.css';

interface Tramo {
  clave: number;
  inicio: string;
  fin: string;
}

const ORDINALES = ['', '1.er', '2.º', '3.er', '4.º', '5.º'];

// Clave de cada tramo en la lista, para que React no confunda uno con otro al quitar alguno
let ultimaClave = 0;
const nuevaClave = () => ++ultimaClave;

/**
 * Los turnos de toda la semana de un odontólogo: cada día, ninguno, uno o varios tramos (por ejemplo, mañana y
 * tarde). La API comprueba que no se pisan y que van en cuartos de hora.
 */
export function DialogoTurnos({
  odontologo,
  clinica,
  alCerrar,
  alTerminar,
}: PropsDialogo & { odontologo: TurnosDeOdontologo; clinica: DiaDeApertura[] }) {
  const refrescar = useRefrescarConfiguracion();
  const [tramos, setTramos] = useState<Record<number, Tramo[]>>(() =>
    Object.fromEntries(
      DIAS.map(({ numero }) => [
        numero,
        odontologo.turnos
          .filter((t) => t.dia_semana === numero)
          .map((t) => ({ clave: nuevaClave(), inicio: t.hora_inicio, fin: t.hora_fin })),
      ]),
    ),
  );

  const aperturaDel = (dia: number) => clinica.find((d) => d.dia_semana === dia);

  const anadir = (dia: number) => {
    const lista = tramos[dia] ?? [];
    const apertura = aperturaDel(dia);
    const ultimo = lista.at(-1);
    // Se propone el horario de la clínica, o desde donde acaba el último tramo hasta que cierra
    const inicio = ultimo?.fin ?? apertura?.hora_apertura ?? '09:00';
    const cierre = apertura?.hora_cierre ?? '14:00';
    const fin = minutosDe(cierre) > minutosDe(inicio) ? cierre : '';
    setTramos((antes) => ({ ...antes, [dia]: [...lista, { clave: nuevaClave(), inicio, fin }] }));
  };
  const cambiar = (dia: number, clave: number, cambio: Partial<Tramo>) =>
    setTramos((antes) => ({
      ...antes,
      [dia]: (antes[dia] ?? []).map((t) => (t.clave === clave ? { ...t, ...cambio } : t)),
    }));
  const quitar = (dia: number, clave: number) =>
    setTramos((antes) => ({ ...antes, [dia]: (antes[dia] ?? []).filter((t) => t.clave !== clave) }));

  const guardar = useMutation({
    mutationFn: () =>
      cambiarTurnos(
        odontologo.id,
        DIAS.flatMap(({ numero }) =>
          (tramos[numero] ?? []).map((t) => ({ dia_semana: numero, hora_inicio: t.inicio, hora_fin: t.fin })),
        ),
      ),
    onSuccess: async (guardado) => {
      await refrescar();
      alTerminar({
        mensaje: `Se han guardado los turnos de ${odontologo.nombre}. La web y la agenda ya ofrecen sus huecos nuevos.`,
        afectadas: { citas: guardado.citas_afectadas, motivo: 'quedan fuera de sus turnos nuevos' },
      });
    },
  });

  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, ['turnos']);

  return (
    <Dialogo
      titulo={`Turnos de ${odontologo.nombre}`}
      ancho="amplio"
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            Guardar turnos
          </Boton>
        </>
      }
    >
      <p className={p.secundario}>
        Un día puede tener varios tramos, por ejemplo de mañana y de tarde. Un día sin tramos no trabaja. Las
        horas van en punto o en cuartos de hora.
      </p>
      <div className={css.dias}>
        {DIAS.map(({ numero, nombre }) => {
          const lista = tramos[numero] ?? [];
          const apertura = aperturaDel(numero);
          return (
            <fieldset key={numero} className={css.diaDeTurnos}>
              <legend>
                {conMayuscula(nombre)}
                <span className={css.nota}>
                  {apertura
                    ? `La clínica abre de ${tramo(apertura.hora_apertura, apertura.hora_cierre)}`
                    : 'La clínica no abre'}
                </span>
              </legend>
              {lista.length === 0 && <span className={p.secundario}>No trabaja</span>}
              {lista.map((t, i) => {
                const cual = lista.length > 1 ? ` (${ORDINALES[i + 1] ?? `${i + 1}.º`} tramo)` : '';
                return (
                  <div key={t.clave} className={css.horas}>
                    <input
                      type="time"
                      step={900}
                      className={ui.control}
                      aria-label={`Empieza el ${nombre}${cual}`}
                      value={t.inicio}
                      onChange={(e) => cambiar(numero, t.clave, { inicio: e.target.value })}
                    />
                    <span aria-hidden="true">a</span>
                    <input
                      type="time"
                      step={900}
                      className={ui.control}
                      aria-label={`Termina el ${nombre}${cual}`}
                      value={t.fin}
                      onChange={(e) => cambiar(numero, t.clave, { fin: e.target.value })}
                    />
                    <Boton variante="texto" pequeno onClick={() => quitar(numero, t.clave)}>
                      Quitar
                      <TextoOculto>
                        el tramo del {nombre}
                        {cual}
                      </TextoOculto>
                    </Boton>
                  </div>
                );
              })}
              <div>
                <Boton variante="texto" pequeno onClick={() => anadir(numero)}>
                  {lista.length > 0 ? 'Añadir otro tramo' : 'Añadir tramo'}
                  <TextoOculto>el {nombre}</TextoOculto>
                </Boton>
              </div>
            </fieldset>
          );
        })}
      </div>
      {errores.turnos && <Aviso tipo="error">{errores.turnos}</Aviso>}
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
