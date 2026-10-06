import { useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import type { Bloqueo, CitasAfectadas, TipoDeBloqueo } from '../../shared/api/tipos';
import { conMayuscula, formatearDia } from '../../shared/fechas';
import { formatearFecha } from '../../shared/formato';

/** Los días como los numera la API: 1 = lunes … 7 = domingo. */
export const DIAS = [
  { numero: 1, nombre: 'lunes' },
  { numero: 2, nombre: 'martes' },
  { numero: 3, nombre: 'miércoles' },
  { numero: 4, nombre: 'jueves' },
  { numero: 5, nombre: 'viernes' },
  { numero: 6, nombre: 'sábado' },
  { numero: 7, nombre: 'domingo' },
] as const;

export function nombreDelDia(numero: number): string {
  return DIAS.find((dia) => dia.numero === numero)?.nombre ?? '';
}

/** «09:00 a 13:00» */
export function tramo(inicio: string, fin: string): string {
  return `${inicio} a ${fin}`;
}

/** Los tipos de bloqueo en el orden en que se ofrecen (los más habituales primero). */
export const TIPOS_DE_BLOQUEO: TipoDeBloqueo[] = [
  'FERIADO',
  'VACACIONES',
  'REUNION',
  'CAPACITACION',
  'MANTENIMIENTO',
  'ALMUERZO',
  'MANUAL',
];

/** Cuándo se aplica un bloqueo: «Cada miércoles», «Martes 13 de octubre de 2026», «Del 12/10/2026 al 16/10/2026»… */
export function cuandoDelBloqueo(bloqueo: Bloqueo): string {
  const { fecha_inicio: desde, fecha_fin: hasta, dia_semana: dia } = bloqueo;
  if (dia) {
    const cada = `Cada ${nombreDelDia(dia)}`;
    if (desde && hasta) return `${cada}, del ${formatearFecha(desde)} al ${formatearFecha(hasta)}`;
    if (desde) return `${cada}, desde el ${formatearFecha(desde)}`;
    if (hasta) return `${cada}, hasta el ${formatearFecha(hasta)}`;
    return cada;
  }
  if (desde && (!hasta || hasta === desde)) return conMayuscula(formatearDia(desde, true));
  if (desde && hasta) return `Del ${formatearFecha(desde)} al ${formatearFecha(hasta)}`;
  return 'Todos los días';
}

export function horasDelBloqueo(bloqueo: Bloqueo): string {
  return bloqueo.hora_inicio && bloqueo.hora_fin
    ? tramo(bloqueo.hora_inicio, bloqueo.hora_fin)
    : 'Todo el día';
}

/** Lo que se acaba de hacer: el mensaje para la página y, si las hay, las citas que conviene revisar. */
export interface Resultado {
  mensaje: ReactNode;
  afectadas?: {
    citas: CitasAfectadas;
    /** Termina la frase «Hay 2 citas pendientes o confirmadas que…» */
    motivo: string;
  };
}

/** Props comunes de los diálogos de configuración. */
export interface PropsDialogo {
  alCerrar: () => void;
  alTerminar: (resultado: Resultado) => void;
}

/**
 * Tras un cambio: las pantallas de configuración y todo lo que depende de ellas (la agenda, los huecos libres y
 * las listas de tratamientos y odontólogos que usan los formularios de citas).
 */
export function useRefrescarConfiguracion(): () => Promise<unknown> {
  const queryClient = useQueryClient();
  return () =>
    Promise.all(
      ['configuracion', 'agenda', 'huecos', 'tratamientos', 'odontologos'].map((clave) =>
        queryClient.invalidateQueries({ queryKey: [clave] }),
      ),
    );
}
