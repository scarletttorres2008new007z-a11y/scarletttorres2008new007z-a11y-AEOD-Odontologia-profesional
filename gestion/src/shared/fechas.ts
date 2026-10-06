/**
 * Días para moverse por la agenda. Las fechas viajan como texto aaaa-mm-dd y la clínica trabaja en hora de Madrid:
 * «hoy» se calcula en Madrid aunque el ordenador tenga otra zona horaria.
 * Aquí solo se cuentan y se escriben días; qué huecos hay libres lo decide siempre la API.
 */

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
const MESES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];

const MADRID = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Europe/Madrid',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

/** Hoy en Madrid, como aaaa-mm-dd. */
export function hoyEnMadrid(ahora: Date = new Date()): string {
  return MADRID.format(ahora);
}

/** aaaa-mm-dd → medianoche UTC, para que sumar días no dependa de los cambios de hora. */
function aFecha(texto: string): Date {
  const [anio = 0, mes = 1, dia = 1] = texto.split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia));
}

/** true si es una fecha aaaa-mm-dd que existe (no vale 2026-02-31). */
export function esFecha(texto: string | null | undefined): texto is string {
  if (!texto || !/^\d{4}-\d{2}-\d{2}$/.test(texto)) return false;
  return aFecha(texto).toISOString().slice(0, 10) === texto;
}

export function sumarDias(fecha: string, dias: number): string {
  const resultado = aFecha(fecha);
  resultado.setUTCDate(resultado.getUTCDate() + dias);
  return resultado.toISOString().slice(0, 10);
}

/** El lunes de la semana de esa fecha. */
export function lunesDe(fecha: string): string {
  return sumarDias(fecha, -((aFecha(fecha).getUTCDay() + 6) % 7));
}

/** «lunes 12 de octubre», o «lunes 12 de octubre de 2026» con el año. */
export function formatearDia(fecha: string, conAnio = false): string {
  const dia = aFecha(fecha);
  const texto = `${DIAS[dia.getUTCDay()]} ${dia.getUTCDate()} de ${MESES[dia.getUTCMonth()]}`;
  return conAnio ? `${texto} de ${dia.getUTCFullYear()}` : texto;
}

/** «mié 7 oct», para listas estrechas. */
export function formatearDiaCorto(fecha: string): string {
  const dia = aFecha(fecha);
  return `${DIAS[dia.getUTCDay()]?.slice(0, 3)} ${dia.getUTCDate()} ${MESES[dia.getUTCMonth()]?.slice(0, 3)}`;
}

/** «del 12 al 18 de octubre de 2026» o «del 28 de septiembre al 4 de octubre de 2026». */
export function formatearSemana(lunes: string): string {
  const inicio = aFecha(lunes);
  const fin = aFecha(sumarDias(lunes, 6));
  const mesInicio = MESES[inicio.getUTCMonth()];
  const mesFin = MESES[fin.getUTCMonth()];
  const anioFin = fin.getUTCFullYear();
  if (inicio.getUTCFullYear() !== anioFin) {
    return `del ${inicio.getUTCDate()} de ${mesInicio} de ${inicio.getUTCFullYear()} al ${fin.getUTCDate()} de ${mesFin} de ${anioFin}`;
  }
  const desde = mesInicio === mesFin ? `${inicio.getUTCDate()}` : `${inicio.getUTCDate()} de ${mesInicio}`;
  return `del ${desde} al ${fin.getUTCDate()} de ${mesFin} de ${anioFin}`;
}

/** «10:30» → 630 minutos desde las 00:00. */
export function minutosDe(hora: string): number {
  const [horas = 0, minutos = 0] = hora.split(':').map(Number);
  return horas * 60 + minutos;
}

/** 630 → «10:30». */
export function horaDe(minutos: number): string {
  return `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`;
}

export function conMayuscula(texto: string): string {
  return texto.charAt(0).toLocaleUpperCase('es') + texto.slice(1);
}
