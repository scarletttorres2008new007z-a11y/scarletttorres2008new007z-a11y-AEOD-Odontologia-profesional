import type { ButtonHTMLAttributes } from 'react';
import css from './ui.module.css';

type Variante = 'primario' | 'secundario' | 'peligro' | 'texto';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante;
  pequeno?: boolean;
  /** Muestra un indicador y desactiva el botón mientras se guarda. */
  cargando?: boolean;
}

export function Boton({
  variante = 'primario',
  pequeno = false,
  cargando = false,
  type = 'button',
  className,
  disabled,
  children,
  ...resto
}: Props) {
  const clases = [css.boton, css[variante], pequeno ? css.pequeno : '', className ?? ''].join(' ').trim();
  return (
    <button
      type={type}
      className={clases}
      disabled={disabled || cargando}
      aria-busy={cargando || undefined}
      {...resto}
    >
      {cargando && <span className={css.girando} aria-hidden="true" />}
      {children}
    </button>
  );
}
