/**
 * Fechas de la API: llegan en hora de Madrid sin zona ("2026-10-12T08:05:30.123"), así que se muestran tal cual,
 * sin convertirlas a la zona del ordenador.
 */
const FECHA_HORA = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2})(?::(\d{2}))?)?/;

export function formatearFechaHora(valor: string | null | undefined, conSegundos = false): string {
  const partes = valor ? FECHA_HORA.exec(valor) : null;
  if (!partes) return '—';
  const [, anio, mes, dia, hora, minuto, segundo] = partes;
  const fecha = `${dia}/${mes}/${anio}`;
  if (!hora) return fecha;
  return `${fecha} ${hora}:${minuto}${conSegundos ? `:${segundo ?? '00'}` : ''}`;
}

export function formatearFecha(valor: string | null | undefined): string {
  return formatearFechaHora(valor?.slice(0, 10));
}

/**
 * Teléfonos como los guarda la API (solo dígitos, con + si llevan prefijo) en grupos fáciles de leer:
 * "600123456" → "600 123 456" y "+34600123456" → "+34 600 123 456". Los demás se dejan tal cual.
 */
export function formatearTelefono(telefono: string | null | undefined): string {
  if (!telefono) return '';
  const espanol = /^(\+34)?(\d{3})(\d{3})(\d{3})$/.exec(telefono);
  if (!espanol) return telefono;
  const [, prefijo, a, b, c] = espanol;
  return `${prefijo ? `${prefijo} ` : ''}${a} ${b} ${c}`;
}
