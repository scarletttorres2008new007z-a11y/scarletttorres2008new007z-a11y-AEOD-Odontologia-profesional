import { api } from '../../shared/api/cliente';
import type { DatosPaciente, PaginaPacientes, Paciente } from '../../shared/api/tipos';

export const TAMANO_PAGINA = 20;

export interface FiltroPacientes {
  texto: string;
  /** '' = todos, 'true' = activos, 'false' = dados de baja */
  activo: '' | 'true' | 'false';
  pagina: number;
}

export function buscarPacientes(
  filtro: FiltroPacientes,
  signal?: AbortSignal,
  tamano = TAMANO_PAGINA,
): Promise<PaginaPacientes> {
  return api<PaginaPacientes>('/api/pacientes', {
    parametros: { texto: filtro.texto, activo: filtro.activo, pagina: filtro.pagina, tamano },
    signal,
  });
}

export function obtenerPaciente(id: number, signal?: AbortSignal): Promise<Paciente> {
  return api<Paciente>(`/api/pacientes/${id}`, { signal });
}

export function crearPaciente(datos: DatosPaciente): Promise<Paciente> {
  return api<Paciente>('/api/pacientes', { metodo: 'POST', cuerpo: datos });
}

/** Sustituye todos sus datos: lo que llegue vacío se borra. */
export function editarPaciente(id: number, datos: DatosPaciente): Promise<Paciente> {
  return api<Paciente>(`/api/pacientes/${id}`, { metodo: 'PUT', cuerpo: datos });
}

/** Dar de baja no borra nada; se puede reactivar cuando se quiera. */
export function cambiarEstadoDelPaciente(id: number, activo: boolean): Promise<Paciente> {
  return api<Paciente>(`/api/pacientes/${id}/estado`, { metodo: 'PUT', cuerpo: { activo } });
}
