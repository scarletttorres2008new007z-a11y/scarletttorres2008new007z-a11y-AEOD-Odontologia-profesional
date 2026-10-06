import { api } from '../../shared/api/cliente';
import type { Permiso, Rol } from '../../shared/api/tipos';

export const CLAVE_ROLES = ['roles'] as const;
export const CLAVE_PERMISOS = ['permisos'] as const;

/** Exige roles.ver o usuarios.asignar_roles (para elegir los roles de un usuario). */
export function listarRoles(): Promise<Rol[]> {
  return api<Rol[]>('/api/roles');
}

export function listarPermisos(): Promise<Permiso[]> {
  return api<Permiso[]>('/api/permisos');
}

export function cambiarPermisosDelRol(id: number, permisos: string[]): Promise<Rol> {
  return api<Rol>(`/api/roles/${id}/permisos`, { metodo: 'PUT', cuerpo: { permisos } });
}
