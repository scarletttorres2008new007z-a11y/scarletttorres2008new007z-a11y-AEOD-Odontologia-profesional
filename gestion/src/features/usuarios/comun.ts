import { useQueryClient } from '@tanstack/react-query';
import type { Usuario } from '../../shared/api/tipos';

export const ADMINISTRADOR = 'ADMINISTRADOR';

/** Props comunes de los diálogos de la página de usuarios. */
export interface PropsDialogo {
  alCerrar: () => void;
  /** Se llama al guardar, con el mensaje que confirma lo que se ha hecho. */
  alTerminar: (mensaje: string) => void;
}

/** Tras cambiar un usuario: la lista, el número de usuarios de cada rol y (por si era uno mismo) la sesión. */
export function useRefrescarUsuarios(): () => Promise<unknown> {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['usuarios'] }),
      queryClient.invalidateQueries({ queryKey: ['roles'] }),
      queryClient.invalidateQueries({ queryKey: ['usuario-actual'] }),
    ]);
}

/**
 * Roles que la persona no puede marcar ni desmarcar, con el motivo. Es una ayuda: la API aplica las mismas
 * reglas y rechaza el cambio aunque se intente por otro camino.
 */
export function rolesBloqueados(soyAdministrador: boolean, usuario?: Usuario, yo?: { id: number }) {
  if (!soyAdministrador) {
    return { [ADMINISTRADOR]: 'Solo un administrador puede dar o quitar este rol.' };
  }
  const esMiUsuario = usuario !== undefined && usuario.id === yo?.id;
  if (esMiUsuario && usuario.roles.some((rol) => rol.codigo === ADMINISTRADOR)) {
    return { [ADMINISTRADOR]: 'No puedes quitarte tu propio rol de administrador.' };
  }
  return {};
}
