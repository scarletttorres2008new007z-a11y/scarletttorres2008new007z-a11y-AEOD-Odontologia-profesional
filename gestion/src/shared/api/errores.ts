/**
 * Error de una llamada a la API. `message` ya viene preparado para mostrarlo a la persona;
 * `errores` trae un mensaje por campo cuando lo que falla son los datos de un formulario.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly errores: Record<string, string>;

  constructor(status: number, message: string, errores: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errores = errores;
  }
}

export const SIN_CONEXION =
  'No se puede conectar con el servidor. Comprueba que el backend está arrancado y vuelve a intentarlo.';

/** Mensaje que se puede enseñar para cualquier error. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return 'Ha ocurrido un error inesperado. Inténtalo de nuevo.';
}

/** Errores por campo de un formulario (vacío si el error no es de datos). */
export function erroresDeCampos(error: unknown): Record<string, string> {
  return error instanceof ApiError ? error.errores : {};
}

/**
 * Mensaje para el aviso general de un formulario. Queda vacío si el error ya se enseña junto a alguno de los
 * campos del formulario (así no se repite).
 */
export function mensajeGeneral(error: unknown, campos: readonly string[]): string {
  if (!error) return '';
  const errores = erroresDeCampos(error);
  return campos.some((campo) => errores[campo]) ? '' : mensajeDeError(error);
}
