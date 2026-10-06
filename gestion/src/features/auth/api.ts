import { api, guardarToken } from '../../shared/api/cliente';
import type { Sesion, UsuarioActual } from '../../shared/api/tipos';

export async function iniciarSesion(usuario: string, password: string): Promise<Sesion> {
  const sesion = await api<Sesion>('/api/auth/login', {
    metodo: 'POST',
    cuerpo: { usuario, password },
    sinRenovar: true,
  });
  guardarToken(sesion.token_acceso);
  return sesion;
}

/** Cierra la sesión en el servidor (anula la cookie). Si falla, la sesión sigue abierta y se lanza el error. */
export async function cerrarSesion(): Promise<void> {
  await api<void>('/api/auth/logout', { metodo: 'POST', sinRenovar: true });
  guardarToken(null);
}

export function obtenerUsuarioActual(): Promise<UsuarioActual> {
  return api<UsuarioActual>('/api/auth/me');
}

export function cambiarMiPassword(passwordActual: string, passwordNueva: string): Promise<void> {
  return api<void>('/api/auth/password', {
    metodo: 'PUT',
    cuerpo: { password_actual: passwordActual, password_nueva: passwordNueva },
  });
}
