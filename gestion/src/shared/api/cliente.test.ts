import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { respuestaJson } from '../../test/utilidades';
import { api, avisarAlTerminarSesion, guardarToken } from './cliente';
import { ApiError, SIN_CONEXION } from './errores';

const SESION = {
  token_acceso: 'token-nuevo',
  tipo: 'Bearer',
  expira_en: 900,
  usuario: { id: 1, username: 'admin', email: 'a@example.com', nombre: 'Admin', roles: [], permisos: [] },
};

const fetchFalso = vi.fn<typeof fetch>();

function cabeceras(llamada: number): Record<string, string> {
  return (fetchFalso.mock.calls[llamada]?.[1]?.headers ?? {}) as Record<string, string>;
}

function url(llamada: number): string {
  return String(fetchFalso.mock.calls[llamada]?.[0]);
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchFalso);
  guardarToken('token-viejo');
});

afterEach(() => {
  fetchFalso.mockReset();
  vi.unstubAllGlobals();
  avisarAlTerminarSesion(null);
  guardarToken(null);
});

describe('cliente de la API', () => {
  it('envía el token y la cabecera del software', async () => {
    fetchFalso.mockResolvedValueOnce(respuestaJson({ ok: true }));

    await api('/api/usuarios', { parametros: { texto: 'ana', activo: '', pagina: 0 } });

    expect(url(0)).toBe('/api/usuarios?texto=ana&pagina=0');
    expect(cabeceras(0)).toMatchObject({
      Authorization: 'Bearer token-viejo',
      'X-Requested-With': 'XMLHttpRequest',
    });
  });

  it('si el token caduca, lo renueva con la cookie y repite la petición', async () => {
    fetchFalso
      .mockResolvedValueOnce(respuestaJson({ message: 'Tu sesión ha caducado.' }, 401))
      .mockResolvedValueOnce(respuestaJson(SESION))
      .mockResolvedValueOnce(respuestaJson({ total: 3 }));

    await expect(api('/api/usuarios')).resolves.toEqual({ total: 3 });

    expect(url(1)).toBe('/api/auth/refresh');
    expect(cabeceras(2).Authorization).toBe('Bearer token-nuevo');
  });

  it('varias peticiones caducadas a la vez renuevan una sola vez', async () => {
    fetchFalso.mockImplementation((entrada, init) => {
      const ruta = String(entrada);
      if (ruta === '/api/auth/refresh') return Promise.resolve(respuestaJson(SESION));
      const token = new Headers(init?.headers).get('Authorization');
      return Promise.resolve(
        token === 'Bearer token-nuevo' ? respuestaJson({ ruta }) : respuestaJson({}, 401),
      );
    });

    await Promise.all([api('/api/usuarios'), api('/api/roles'), api('/api/auditoria')]);

    const renovaciones = fetchFalso.mock.calls.filter(([entrada]) => String(entrada) === '/api/auth/refresh');
    expect(renovaciones).toHaveLength(1);
  });

  it('si la sesión ya no vale, avisa para volver a la pantalla de entrada', async () => {
    const alTerminar = vi.fn<() => void>();
    avisarAlTerminarSesion(alTerminar);
    fetchFalso
      .mockResolvedValueOnce(respuestaJson({ message: 'Inicia sesión para continuar.' }, 401))
      .mockResolvedValueOnce(respuestaJson({ message: 'Tu sesión ha terminado.' }, 401));

    await expect(api('/api/usuarios')).rejects.toMatchObject({ status: 401 });
    expect(alTerminar).toHaveBeenCalledOnce();
  });

  it('las rutas de sesión no intentan renovar', async () => {
    fetchFalso.mockResolvedValueOnce(respuestaJson({ message: 'Usuario o contraseña incorrectos.' }, 401));

    await expect(api('/api/auth/login', { metodo: 'POST', cuerpo: {}, sinRenovar: true })).rejects.toThrow(
      'Usuario o contraseña incorrectos.',
    );
    expect(fetchFalso).toHaveBeenCalledOnce();
  });

  it('convierte el error de la API en ApiError con su mensaje y los errores por campo', async () => {
    fetchFalso.mockResolvedValueOnce(
      respuestaJson(
        {
          timestamp: '2026-10-12T10:00:00+02:00',
          status: 409,
          error: 'CONFLICT',
          message: 'Ya existe un usuario con ese nombre.',
          path: '/api/usuarios',
          errores: { username: 'Ya existe un usuario con ese nombre.' },
        },
        409,
      ),
    );

    const error = await api('/api/usuarios', { metodo: 'POST', cuerpo: {} }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 409,
      message: 'Ya existe un usuario con ese nombre.',
      errores: { username: 'Ya existe un usuario con ese nombre.' },
    });
  });

  it('sin servidor, explica que no hay conexión', async () => {
    fetchFalso.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    await expect(api('/api/usuarios')).rejects.toMatchObject({ status: 0, message: SIN_CONEXION });

    // El proxy de Vite responde 500 sin JSON cuando el backend está apagado
    fetchFalso.mockResolvedValueOnce(new Response('Error', { status: 500 }));
    await expect(api('/api/usuarios')).rejects.toMatchObject({ status: 500, message: SIN_CONEXION });
  });

  it('una respuesta 204 no tiene cuerpo', async () => {
    fetchFalso.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(api('/api/auth/password', { metodo: 'PUT', cuerpo: {} })).resolves.toBeUndefined();
  });
});
