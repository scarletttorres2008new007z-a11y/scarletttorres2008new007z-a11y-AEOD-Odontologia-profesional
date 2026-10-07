import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router';
import { vi } from 'vitest';
import { AuthContexto, type Auth } from '../features/auth/contexto';
import type { CodigoPermiso } from '../features/auth/permisos';
import type { UsuarioActual } from '../shared/api/tipos';

export function usuarioDePrueba(
  permisos: CodigoPermiso[] = [],
  roles: UsuarioActual['roles'] = [{ codigo: 'RECEPCION', nombre: 'Recepción' }],
): UsuarioActual {
  return { id: 7, username: 'marta', email: 'marta@example.com', nombre: 'Marta Sánchez', roles, permisos };
}

/** Pinta un componente con la sesión indicada, la caché de consultas y el router en la ruta pedida. */
export function pintar(
  ui: ReactElement,
  { ruta = '/', auth = {} }: { ruta?: string; auth?: Partial<Auth> } = {},
) {
  const usuario = auth.usuario ?? null;
  const valor: Auth = {
    estado: usuario ? 'con-sesion' : 'sin-sesion',
    usuario,
    sesionTerminada: false,
    entrar: vi.fn<Auth['entrar']>(() => Promise.resolve()),
    salir: vi.fn<Auth['salir']>(() => Promise.resolve()),
    reintentar: vi.fn<Auth['reintentar']>(),
    tienePermiso: (permiso) => usuario?.permisos.includes(permiso) ?? false,
    ...auth,
  };
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const resultado = render(
    <QueryClientProvider client={cliente}>
      <AuthContexto.Provider value={valor}>
        <MemoryRouter initialEntries={[ruta]}>{ui}</MemoryRouter>
      </AuthContexto.Provider>
    </QueryClientProvider>,
  );
  return { ...resultado, auth: valor };
}

/** Respuesta JSON como la de la API. */
export function respuestaJson(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': 'application/json' } });
}
