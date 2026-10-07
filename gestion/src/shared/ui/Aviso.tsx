import type { ReactNode } from 'react';
import css from './ui.module.css';

type Tipo = 'error' | 'exito' | 'info' | 'alerta';

/** Mensaje destacado. Los errores se anuncian al momento a los lectores de pantalla. */
export function Aviso({ tipo = 'info', children }: { tipo?: Tipo; children: ReactNode }) {
  return (
    <div className={css.aviso} data-tipo={tipo} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className={css.cargando} role="status">
      <span className={css.girando} aria-hidden="true" />
      {texto}
    </div>
  );
}

export function Vacio({ children }: { children: ReactNode }) {
  return <div className={css.vacio}>{children}</div>;
}
