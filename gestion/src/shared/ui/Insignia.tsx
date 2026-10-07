import type { ReactNode } from 'react';
import css from './ui.module.css';

export type Tono = 'neutro' | 'marca' | 'exito' | 'alerta' | 'error';

export function Insignia({ tono = 'neutro', children }: { tono?: Tono; children: ReactNode }) {
  return (
    <span className={css.insignia} data-tono={tono}>
      {children}
    </span>
  );
}
