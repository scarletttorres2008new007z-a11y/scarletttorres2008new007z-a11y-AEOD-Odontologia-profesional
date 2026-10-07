import { describe, expect, it } from 'vitest';
import { formatearEuros, formatearFecha, formatearFechaHora, formatearTelefono } from './formato';

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

describe('formato de teléfonos', () => {
  it('agrupa los números españoles de tres en tres', () => {
    expect(formatearTelefono('600123456')).toBe('600 123 456');
    expect(formatearTelefono('+34600123456')).toBe('+34 600 123 456');
  });

  it('deja tal cual los demás y vacío si no hay', () => {
    expect(formatearTelefono('+442071234567')).toBe('+442071234567');
    expect(formatearTelefono(undefined)).toBe('');
  });
});

describe('formato de precios', () => {
  it('euros sin céntimos si no los tiene, con dos si los tiene', () => {
    expect(formatearEuros(60)).toBe('60\u00a0€');
    expect(formatearEuros(49.5)).toBe('49,50\u00a0€');
    expect(formatearEuros(0)).toBe('0\u00a0€');
  });
});
