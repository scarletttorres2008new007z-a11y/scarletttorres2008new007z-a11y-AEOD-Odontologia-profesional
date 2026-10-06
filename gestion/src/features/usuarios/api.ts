import { api } from '../../shared/api/cliente';
import type { PaginaUsuarios, Usuario, UsuarioCreacion, UsuarioEdicion } from '../../shared/api/tipos';

export const TAMANO_PAGINA = 20;

export interface FiltroUsuarios {
  texto: string;
  /** '' = todos, 'true' = activos, 'false' = desactivados */
  activo: '' | 'true' | 'false';
  pagina: number;
}

export function buscarUsuarios(filtro: FiltroUsuarios, signal?: AbortSignal): Promise<PaginaUsuarios> {
  return api<PaginaUsuarios>('/api/usuarios', {
    parametros: { texto: filtro.texto, activo: filtro.activo, pagina: filtro.pagina, tamano: TAMANO_PAGINA },
    signal,
  });
}

export function crearUsuario(datos: UsuarioCreacion): Promise<Usuario> {
  return api<Usuario>('/api/usuarios', { metodo: 'POST', cuerpo: datos });
}

export function editarUsuario(id: number, datos: UsuarioEdicion): Promise<Usuario> {
  return api<Usuario>(`/api/usuarios/${id}`, { metodo: 'PUT', cuerpo: datos });
}

/** Desactivar cierra sus sesiones al momento; activar quita también un bloqueo por intentos fallidos. */
export function cambiarEstadoDelUsuario(id: number, activo: boolean): Promise<Usuario> {
  return api<Usuario>(`/api/usuarios/${id}/estado`, { metodo: 'PUT', cuerpo: { activo } });
}

export function cambiarRolesDelUsuario(id: number, roles: string[]): Promise<Usuario> {
  return api<Usuario>(`/api/usuarios/${id}/roles`, { metodo: 'PUT', cuerpo: { roles } });
}

/** Cierra todas sus sesiones abiertas. */
export function restablecerPassword(id: number, password: string): Promise<void> {
  return api<void>(`/api/usuarios/${id}/password`, { metodo: 'PUT', cuerpo: { password } });
}
