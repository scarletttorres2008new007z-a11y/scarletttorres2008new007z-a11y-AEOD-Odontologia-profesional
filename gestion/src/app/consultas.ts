import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { ApiError } from '../shared/api/errores';

/**
 * Caché de las consultas a la API.
 * - Un error 4xx no se reintenta: repetir la misma petición daría el mismo error.
 * - Si la API responde 403 (por ejemplo, a alguien le acaban de quitar un permiso), se vuelven a pedir sus
 *   permisos para que el menú deje de ofrecer lo que ya no puede hacer.
 */
export function crearClienteDeConsultas(): QueryClient {
  const alFallar = (error: Error) => {
    if (error instanceof ApiError && error.status === 403) {
      void cliente.invalidateQueries({ queryKey: ['usuario-actual'] });
    }
  };
  const cliente: QueryClient = new QueryClient({
    queryCache: new QueryCache({ onError: alFallar }),
    mutationCache: new MutationCache({ onError: alFallar }),
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: (fallos, error) => {
          if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false;
          return fallos < 2;
        },
      },
    },
  });
  return cliente;
}
