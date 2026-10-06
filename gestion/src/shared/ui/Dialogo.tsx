import { useEffect, useId, useRef, type FormEvent, type ReactNode } from 'react';
import css from './ui.module.css';

interface Props {
  titulo: string;
  alCerrar: () => void;
  children: ReactNode;
  /** Botones del pie. Si hay alEnviar, el diálogo es un formulario y Enter lo envía. */
  pie: ReactNode;
  alEnviar?: () => void;
  /** 'amplio' para formularios largos, con los campos en dos columnas. */
  ancho?: 'normal' | 'amplio';
}

/**
 * Ventana modal con el elemento <dialog> del navegador: bloquea el resto de la página, mantiene el foco
 * dentro y se cierra con Escape. Se muestra mientras está montada.
 */
export function Dialogo({ titulo, alCerrar, children, pie, alEnviar, ancho = 'normal' }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const idTitulo = useId();

  useEffect(() => {
    const dialogo = ref.current;
    // Al cerrar, el foco vuelve a donde estaba (normalmente, el botón que abrió el diálogo)
    const anterior = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    if (dialogo && !dialogo.open) {
      dialogo.showModal?.();
      // El navegador enfocaría el botón de cerrar; mejor el primer campo, para escribir directamente
      dialogo
        .querySelector<HTMLElement>(`.${css.dialogoCuerpo} :is(input, select, textarea):not(:disabled)`)
        ?.focus();
    }
    return () => {
      dialogo?.close?.();
      if (anterior?.isConnected) anterior.focus();
    };
  }, []);

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    alEnviar?.();
  };

  const contenido = (
    <>
      <div className={css.dialogoCabecera}>
        <h2 id={idTitulo}>{titulo}</h2>
        <button type="button" className={css.cerrar} onClick={alCerrar} aria-label="Cerrar">
          ×
        </button>
      </div>
      <div className={css.dialogoCuerpo}>{children}</div>
      <div className={css.dialogoPie}>{pie}</div>
    </>
  );

  return (
    <dialog
      ref={ref}
      className={css.dialogo}
      data-ancho={ancho}
      aria-labelledby={idTitulo}
      onCancel={(evento) => {
        evento.preventDefault();
        alCerrar();
      }}
    >
      {alEnviar ? (
        <form onSubmit={enviar} noValidate>
          {contenido}
        </form>
      ) : (
        contenido
      )}
    </dialog>
  );
}
