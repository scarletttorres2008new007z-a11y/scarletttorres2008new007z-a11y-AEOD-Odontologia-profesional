import { useMutation } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Dialogo } from '../../shared/ui/Dialogo';
import { useRefrescarConfiguracion } from './comun';

interface Props {
  titulo: string;
  /** Qué pasará si se confirma. */
  children: ReactNode;
  boton: string;
  peligro?: boolean;
  accion: () => Promise<unknown>;
  alCerrar: () => void;
  alTerminar: () => void;
}

/**
 * Pide confirmación antes de activar, desactivar o quitar algo. Si la API no lo permite (por ejemplo, porque
 * quedan citas pendientes), su motivo se enseña aquí mismo.
 */
export function DialogoConfirmacion({
  titulo,
  children,
  boton,
  peligro,
  accion,
  alCerrar,
  alTerminar,
}: Props) {
  const refrescar = useRefrescarConfiguracion();
  const confirmar = useMutation({
    mutationFn: accion,
    onSuccess: async () => {
      await refrescar();
      alTerminar();
    },
  });

  return (
    <Dialogo
      titulo={titulo}
      alCerrar={alCerrar}
      alEnviar={() => confirmar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" variante={peligro ? 'peligro' : 'primario'} cargando={confirmar.isPending}>
            {boton}
          </Boton>
        </>
      }
    >
      {children}
      {confirmar.error && <Aviso tipo="error">{mensajeDeError(confirmar.error)}</Aviso>}
    </Dialogo>
  );
}
