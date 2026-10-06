import { createContext, useContext } from 'react';
import type { UsuarioActual } from '../../shared/api/tipos';
import type { CodigoPermiso } from './permisos';

export type EstadoSesion = 'comprobando' | 'sin-conexion' | 'sin-sesion' | 'con-sesion';

export interface Auth {
  estado: EstadoSesion;
  usuario: UsuarioActual | null;
  /** La sesión terminó sola (caducó, se cerró en otro equipo, se desactivó el usuario…). */
  sesionTerminada: boolean;
  entrar: (usuario: string, password: string) => Promise<void>;
  salir: () => Promise<void>;
  reintentar: () => void;
  tienePermiso: (permiso: CodigoPermiso) => boolean;
}

export const AuthContexto = createContext<Auth | null>(null);

export function useAuth(): Auth {
  const auth = useContext(AuthContexto);
  if (!auth) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return auth;
}
