import type { ReactNode } from 'react';

/**
 * Texto que solo leen los lectores de pantalla, por ejemplo para que un botón «Editar» se anuncie como
 * «Editar a Marta Sánchez». El espacio va fuera, para que no se pegue al texto visible.
 */
export function TextoOculto({ children }: { children: ReactNode }) {
  return (
    <>
      {' '}
      <span className="visually-hidden">{children}</span>
    </>
  );
}
