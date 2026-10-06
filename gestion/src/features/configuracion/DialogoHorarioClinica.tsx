import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import type { DiaDeApertura } from '../../shared/api/tipos';
import { conMayuscula } from '../../shared/fechas';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Casilla } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import p from '../../shared/ui/pagina.module.css';
import ui from '../../shared/ui/ui.module.css';
import { cambiarHorarioDeLaClinica } from './api';
import { DIAS, useRefrescarConfiguracion, type PropsDialogo } from './comun';
import css from './Configuracion.module.css';

interface Dia {
  abre: boolean;
  apertura: string;
  cierre: string;
}

/** El horario de toda la semana de una vez: qué días abre la clínica y de qué hora a qué hora. */
export function DialogoHorarioClinica({
  clinica,
  alCerrar,
  alTerminar,
}: PropsDialogo & { clinica: DiaDeApertura[] }) {
  const refrescar = useRefrescarConfiguracion();
  // Al abrir un día que estaba cerrado se proponen las horas del primer día que abre (se pueden cambiar)
  const modelo = clinica[0];
  const [dias, setDias] = useState<Record<number, Dia>>(() =>
    Object.fromEntries(
      DIAS.map(({ numero }) => {
        const dia = clinica.find((d) => d.dia_semana === numero);
        return [
          numero,
          {
            abre: dia !== undefined,
            apertura: dia?.hora_apertura ?? modelo?.hora_apertura ?? '09:00',
            cierre: dia?.hora_cierre ?? modelo?.hora_cierre ?? '14:00',
          },
        ];
      }),
    ),
  );

  const cambiar = (numero: number, cambio: Partial<Dia>) =>
    setDias((antes) => ({ ...antes, [numero]: { ...antes[numero]!, ...cambio } }));

  const guardar = useMutation({
    mutationFn: () =>
      cambiarHorarioDeLaClinica(
        DIAS.filter(({ numero }) => dias[numero]?.abre).map(({ numero }) => ({
          dia_semana: numero,
          hora_apertura: dias[numero]!.apertura,
          hora_cierre: dias[numero]!.cierre,
        })),
      ),
    onSuccess: async (guardado) => {
      await refrescar();
      alTerminar({
        mensaje: 'Se ha guardado el horario de la clínica. La web y la agenda ya lo usan.',
        afectadas: { citas: guardado.citas_afectadas, motivo: 'quedan fuera del horario nuevo' },
      });
    },
  });

  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, ['dias']);

  return (
    <Dialogo
      titulo="Horario de la clínica"
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            Guardar horario
          </Boton>
        </>
      }
    >
      <fieldset className={ui.grupo}>
        <legend>Días que abre y horas</legend>
        <div className={css.dias}>
          {DIAS.map(({ numero, nombre }) => {
            const dia = dias[numero]!;
            return (
              <div key={numero} className={css.filaDia}>
                <Casilla
                  etiqueta={conMayuscula(nombre)}
                  checked={dia.abre}
                  onChange={(e) => cambiar(numero, { abre: e.target.checked })}
                />
                {dia.abre ? (
                  <span className={css.horas}>
                    <input
                      type="time"
                      step={900}
                      className={ui.control}
                      aria-label={`Abre el ${nombre} a las`}
                      value={dia.apertura}
                      onChange={(e) => cambiar(numero, { apertura: e.target.value })}
                    />
                    <span aria-hidden="true">a</span>
                    <input
                      type="time"
                      step={900}
                      className={ui.control}
                      aria-label={`Cierra el ${nombre} a las`}
                      value={dia.cierre}
                      onChange={(e) => cambiar(numero, { cierre: e.target.value })}
                    />
                  </span>
                ) : (
                  <span className={p.secundario}>Cerrado</span>
                )}
              </div>
            );
          })}
        </div>
      </fieldset>
      <p className={p.secundario}>
        Las horas van en punto o en cuartos de hora (09:00, 09:15, 09:30…). Las citas que ya estén dadas no se
        cancelan: si alguna queda fuera, se avisa para revisarla.
      </p>
      {errores.dias && <Aviso tipo="error">{errores.dias}</Aviso>}
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
