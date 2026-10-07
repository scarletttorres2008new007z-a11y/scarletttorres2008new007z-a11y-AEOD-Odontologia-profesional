import type { ReactNode } from 'react';
import { Navigate, useLocation, type Location } from 'react-router';
import { useAuth } from '../features/auth/contexto';
import type { CodigoPermiso } from '../features/auth/permisos';
import { PaginaSinPermiso } from './PaginasDeAviso';

interface EstadoEntrada {
  desde?: Location;
}

/** Sin sesión, a la pantalla de entrada; después de entrar se vuelve a la página que se quería ver. */
export function ConSesion({ children }: { children: ReactNode }) {
  const { estado } = useAuth();
  const location = useLocation();
  if (estado !== 'con-sesion') {
    return <Navigate to="/entrar" replace state={{ desde: location } satisfies EstadoEntrada} />;
  }
  return children;
}

/** La pantalla de entrada solo tiene sentido sin sesión. */
export function SoloSinSesion({ children }: { children: ReactNode }) {
  const { estado } = useAuth();
  const location = useLocation();
  if (estado === 'con-sesion') {
    const desde = (location.state as EstadoEntrada | null)?.desde;
    return <Navigate to={desde ? `${desde.pathname}${desde.search}` : '/'} replace />;
  }
  return children;
}

/**
 * Página que exige un permiso. Ocultarla es solo comodidad: si alguien llama a la API sin el permiso,
 * el backend responde 403 de todas formas.
 */
export function ConPermiso({ permiso, children }: { permiso: CodigoPermiso; children: ReactNode }) {
  const { tienePermiso } = useAuth();
  return tienePermiso(permiso) ? children : <PaginaSinPermiso />;
}
