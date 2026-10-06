import { api } from '../../shared/api/cliente';
import type {
  Bloqueo,
  BloqueoGuardado,
  DatosBloqueo,
  DatosOdontologo,
  DatosTratamiento,
  DiaDeApertura,
  Horarios,
  HorariosGuardados,
  OdontologoConfigurado,
  TratamientoConfigurado,
  TurnoSemanal,
  UsuarioParaOdontologo,
} from '../../shared/api/tipos';

// Configuración de la agenda. Cada cambio se aplica al momento en la web y en la agenda. Todas las comprobaciones
// (horas en cuartos de hora, turnos que se pisan, citas pendientes, permisos…) las hace la API.

const BASE = '/api/configuracion';

export const CLAVES = {
  odontologos: ['configuracion', 'odontologos'],
  usuarios: ['configuracion', 'usuarios-para-odontologos'],
  tratamientos: ['configuracion', 'tratamientos'],
  horarios: ['configuracion', 'horarios'],
  bloqueos: ['configuracion', 'bloqueos'],
} as const;

/* ─────────────── Odontólogos ─────────────── */

/** Todos, también los desactivados. */
export function listarOdontologos(signal?: AbortSignal): Promise<OdontologoConfigurado[]> {
  return api<OdontologoConfigurado[]>(`${BASE}/odontologos`, { signal });
}

/** Usuarios activos del software, con el odontólogo al que ya está vinculado cada uno. */
export function listarUsuariosParaVincular(signal?: AbortSignal): Promise<UsuarioParaOdontologo[]> {
  return api<UsuarioParaOdontologo[]>(`${BASE}/odontologos/usuarios`, { signal });
}

export function crearOdontologo(datos: DatosOdontologo): Promise<OdontologoConfigurado> {
  return api<OdontologoConfigurado>(`${BASE}/odontologos`, { metodo: 'POST', cuerpo: datos });
}

export function editarOdontologo(id: number, datos: DatosOdontologo): Promise<OdontologoConfigurado> {
  return api<OdontologoConfigurado>(`${BASE}/odontologos/${id}`, { metodo: 'PUT', cuerpo: datos });
}

/** La API no deja desactivar a quien tiene citas pendientes o confirmadas: responde 409 con el motivo. */
export function cambiarEstadoDelOdontologo(id: number, activo: boolean): Promise<OdontologoConfigurado> {
  return api<OdontologoConfigurado>(`${BASE}/odontologos/${id}/estado`, {
    metodo: 'PUT',
    cuerpo: { activo },
  });
}

/* ─────────────── Tratamientos ─────────────── */

/** Todos, también los que ya no se ofrecen. */
export function listarTratamientos(signal?: AbortSignal): Promise<TratamientoConfigurado[]> {
  return api<TratamientoConfigurado[]>(`${BASE}/tratamientos`, { signal });
}

export function crearTratamiento(datos: DatosTratamiento): Promise<TratamientoConfigurado> {
  return api<TratamientoConfigurado>(`${BASE}/tratamientos`, { metodo: 'POST', cuerpo: datos });
}

export function editarTratamiento(id: number, datos: DatosTratamiento): Promise<TratamientoConfigurado> {
  return api<TratamientoConfigurado>(`${BASE}/tratamientos/${id}`, { metodo: 'PUT', cuerpo: datos });
}

/** Como con los odontólogos: con citas pendientes o confirmadas no se deja de ofrecer (409). */
export function cambiarEstadoDelTratamiento(id: number, activo: boolean): Promise<TratamientoConfigurado> {
  return api<TratamientoConfigurado>(`${BASE}/tratamientos/${id}/estado`, {
    metodo: 'PUT',
    cuerpo: { activo },
  });
}

/* ─────────────── Horarios ─────────────── */

/** El horario de la clínica y los turnos de cada odontólogo activo. */
export function obtenerHorarios(signal?: AbortSignal): Promise<Horarios> {
  return api<Horarios>(`${BASE}/horarios`, { signal });
}

/** Sustituye el horario de toda la semana: los días que no se envían quedan cerrados. */
export function cambiarHorarioDeLaClinica(dias: DiaDeApertura[]): Promise<HorariosGuardados> {
  return api<HorariosGuardados>(`${BASE}/horarios/clinica`, { metodo: 'PUT', cuerpo: { dias } });
}

/** Sustituye todos sus turnos de la semana: un día sin turnos no trabaja. */
export function cambiarTurnos(odontologoId: number, turnos: TurnoSemanal[]): Promise<HorariosGuardados> {
  return api<HorariosGuardados>(`${BASE}/horarios/odontologos/${odontologoId}`, {
    metodo: 'PUT',
    cuerpo: { turnos },
  });
}

/* ─────────────── Bloqueos ─────────────── */

/** Los que siguen en vigor (los que ya pasaron no salen). */
export function listarBloqueos(signal?: AbortSignal): Promise<Bloqueo[]> {
  return api<Bloqueo[]>(`${BASE}/bloqueos`, { signal });
}

export function crearBloqueo(datos: DatosBloqueo): Promise<BloqueoGuardado> {
  return api<BloqueoGuardado>(`${BASE}/bloqueos`, { metodo: 'POST', cuerpo: datos });
}

export function editarBloqueo(id: number, datos: DatosBloqueo): Promise<BloqueoGuardado> {
  return api<BloqueoGuardado>(`${BASE}/bloqueos/${id}`, { metodo: 'PUT', cuerpo: datos });
}

/** No se borra: queda desactivado (y en la auditoría) y ese tiempo vuelve a estar libre. */
export function quitarBloqueo(id: number): Promise<void> {
  return api<void>(`${BASE}/bloqueos/${id}`, { metodo: 'DELETE' });
}
