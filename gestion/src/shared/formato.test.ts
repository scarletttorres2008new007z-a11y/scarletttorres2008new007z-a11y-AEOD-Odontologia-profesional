import { describe, expect, it } from 'vitest';
import { formatearFecha, formatearFechaHora } from './formato';

describe('formato de fechas', () => {
  it('muestra la hora de Madrid tal como llega, sin convertirla', () => {
    expect(formatearFechaHora('2026-10-12T08:05:30.123')).toBe('12/10/2026 08:05');
    expect(formatearFechaHora('2026-10-12T08:05:30.123', true)).toBe('12/10/2026 08:05:30');
    expect(formatearFechaHora('2026-10-12T08:05', true)).toBe('12/10/2026 08:05:00');
  });

  it('solo la fecha', () => {
    expect(formatearFecha('2026-10-12T23:59:00')).toBe('12/10/2026');
    expect(formatearFechaHora('2026-03-01')).toBe('01/03/2026');
  });

  it('sin valor, un guion', () => {
    expect(formatearFechaHora(null)).toBe('—');
    expect(formatearFechaHora(undefined)).toBe('—');
    expect(formatearFecha('')).toBe('—');
    expect(formatearFechaHora('no es una fecha')).toBe('—');
  });
});
