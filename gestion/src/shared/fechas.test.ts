import { describe, expect, it } from 'vitest';
import {
  esFecha,
  formatearDia,
  formatearDiaCorto,
  formatearSemana,
  horaDe,
  hoyEnMadrid,
  lunesDe,
  minutosDe,
  sumarDias,
} from './fechas';

describe('fechas de la agenda', () => {
  it('hoy es el día de Madrid, no el del ordenador', () => {
    // 23:30 en UTC del domingo ya es lunes en Madrid (UTC+2 en octubre)
    expect(hoyEnMadrid(new Date('2026-10-11T23:30:00Z'))).toBe('2026-10-12');
    expect(hoyEnMadrid(new Date('2026-10-12T08:00:00Z'))).toBe('2026-10-12');
  });

  it('suma días sin tropezar con el cambio de hora ni con el cambio de mes', () => {
    expect(sumarDias('2026-10-24', 1)).toBe('2026-10-25');
    expect(sumarDias('2026-10-25', 1)).toBe('2026-10-26');
    expect(sumarDias('2026-10-31', 1)).toBe('2026-11-01');
    expect(sumarDias('2026-01-01', -1)).toBe('2025-12-31');
  });

  it('la semana empieza en lunes', () => {
    expect(lunesDe('2026-10-12')).toBe('2026-10-12');
    expect(lunesDe('2026-10-18')).toBe('2026-10-12');
    expect(lunesDe('2026-10-01')).toBe('2026-09-28');
  });

  it('escribe días y semanas como se leen', () => {
    expect(formatearDia('2026-10-12')).toBe('lunes 12 de octubre');
    expect(formatearDia('2026-10-14', true)).toBe('miércoles 14 de octubre de 2026');
    expect(formatearDiaCorto('2026-10-14')).toBe('mié 14 oct');
    expect(formatearDiaCorto('2026-09-26')).toBe('sáb 26 sep');
    expect(formatearSemana('2026-10-12')).toBe('del 12 al 18 de octubre de 2026');
    expect(formatearSemana('2026-09-28')).toBe('del 28 de septiembre al 4 de octubre de 2026');
    expect(formatearSemana('2026-12-28')).toBe('del 28 de diciembre de 2026 al 3 de enero de 2027');
  });

  it('solo acepta fechas que existen', () => {
    expect(esFecha('2026-10-12')).toBe(true);
    expect(esFecha('2026-02-31')).toBe(false);
    expect(esFecha('12/10/2026')).toBe(false);
    expect(esFecha(null)).toBe(false);
  });

  it('pasa de horas a minutos y al revés', () => {
    expect(minutosDe('10:30')).toBe(630);
    expect(horaDe(630)).toBe('10:30');
    expect(horaDe(540)).toBe('09:00');
  });
});
