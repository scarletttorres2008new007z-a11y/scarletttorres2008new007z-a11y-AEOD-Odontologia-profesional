import { ApiError, SIN_CONEXION } from './errores';
import type { ErrorRespuesta, Sesion } from './tipos';

/**
 * Única puerta del software a la API central. Nunca habla con la base de datos.
 *
 * - El token de acceso vive solo en memoria (no en localStorage): un script ajeno no puede robarlo de ahí.
 * - Al recargar la página o cuando caduca (401), se pide uno nuevo con la cookie de sesión HttpOnly
 *   (POST /api/auth/refresh). Si varias peticiones caducan a la vez, se renueva una sola vez.
 * - Si la sesión ya no vale, se avisa a la aplicación para que vuelva a la pantalla de entrada.
 */

let tokenAcceso: string | null = null;
let renovacionEnCurso: Promise<Sesion | null> | null = null;
let alTerminarSesion: (() => void) | null = null;

const CABECERA_SOFTWARE = { 'X-Requested-With': 'XMLHttpRequest' };

export function guardarToken(token: string | null): void {
  tokenAcceso = token;
}

/** La aplicación indica qué hacer cuando la sesión termina por sí sola (caducada, cerrada en otro sitio…). */
export function avisarAlTerminarSesion(accion: (() => void) | null): void {
  alTerminarSesion = accion;
}

type Parametros = Record<string, string | number | boolean | null | undefined>;

export interface Opciones {
  metodo?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  cuerpo?: unknown;
  parametros?: Parametros;
  /** Para las rutas de sesión: un 401 ahí no se arregla renovando. */
  sinRenovar?: boolean;
  signal?: AbortSignal;
}

export async function api<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  let respuesta = await enviar(ruta, opciones);
  if (respuesta.status === 401 && !opciones.sinRenovar) {
    const sesion = await renovarSesion();
    if (sesion) {
      respuesta = await enviar(ruta, opciones);
    } else {
      alTerminarSesion?.();
    }
  }
  if (!respuesta.ok) throw await errorDe(respuesta);
  if (respuesta.status === 204) return undefined as T;
  return (await respuesta.json()) as T;
}

/**
 * Pide un token nuevo con la cookie de sesión. Devuelve null si no hay sesión abierta.
 * Lanza ApiError si no se puede hablar con el servidor.
 */
export function renovarSesion(): Promise<Sesion | null> {
  renovacionEnCurso ??= (async () => {
    try {
      const respuesta = await llamar('/api/auth/refresh', {
        method: 'POST',
        headers: { Accept: 'application/json', ...CABECERA_SOFTWARE },
      });
      if (respuesta.status === 401) {
        tokenAcceso = null;
        return null;
      }
      if (!respuesta.ok) throw await errorDe(respuesta);
      const sesion = (await respuesta.json()) as Sesion;
      tokenAcceso = sesion.token_acceso;
      return sesion;
    } finally {
      renovacionEnCurso = null;
    }
  })();
  return renovacionEnCurso;
}

function enviar(ruta: string, { metodo = 'GET', cuerpo, parametros, signal }: Opciones): Promise<Response> {
  const cabeceras: Record<string, string> = { Accept: 'application/json', ...CABECERA_SOFTWARE };
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (tokenAcceso) cabeceras.Authorization = `Bearer ${tokenAcceso}`;
  return llamar(ruta + consulta(parametros), {
    method: metodo,
    headers: cabeceras,
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    signal,
  });
}

async function llamar(url: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(url, { ...init, credentials: 'same-origin' });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError(0, SIN_CONEXION);
  }
}

function consulta(parametros?: Parametros): string {
  if (!parametros) return '';
  const valores = new URLSearchParams();
  for (const [clave, valor] of Object.entries(parametros)) {
    if (valor !== undefined && valor !== null && valor !== '') valores.set(clave, String(valor));
  }
  const texto = valores.toString();
  return texto ? `?${texto}` : '';
}

/** Convierte cualquier respuesta de error en ApiError con el mensaje de la API (formato único). */
async function errorDe(respuesta: Response): Promise<ApiError> {
  const tipo = respuesta.headers.get('Content-Type') ?? '';
  if (tipo.includes('application/json')) {
    try {
      const error = (await respuesta.json()) as Partial<ErrorRespuesta>;
      if (typeof error.message === 'string') {
        return new ApiError(respuesta.status, error.message, error.errores ?? {});
      }
    } catch {
      // cuerpo vacío o no válido: se usa el mensaje genérico
    }
  }
  // Sin JSON: el servidor no responde (por ejemplo, el backend está apagado y responde el proxy)
  if (respuesta.status >= 500) return new ApiError(respuesta.status, SIN_CONEXION);
  return new ApiError(respuesta.status, 'Ha ocurrido un error inesperado. Inténtalo de nuevo.');
}
