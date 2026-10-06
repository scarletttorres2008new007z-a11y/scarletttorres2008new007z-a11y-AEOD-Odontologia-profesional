import { api } from '../../shared/api/cliente';
import type {
  Agenda,
  Cita,
  EstadoCita,
  Hueco,
  NuevaCita,
  Odontologo,
  PaginaCitas,
  Reprogramacion,
  Tratamiento,
} from '../../shared/api/tipos';

/** Agenda de uno (vista día) o siete días (vista semana). Sin todas las agendas, la API solo da la propia. */
export function obtenerAgenda(
  desde: string,
  dias: number,
  odontologoId: number | null,
  signal?: AbortSignal,
): Promise<Agenda> {
  return api<Agenda>('/api/agenda', { parametros: { desde, dias, odontologo_id: odontologoId }, signal });
}

export interface FiltroCitas {
  desde?: string;
  hasta?: string;
  estado?: EstadoCita;
  pacienteId?: number;
  /** De la más reciente a la más antigua. */
  recientes?: boolean;
  pagina?: number;
  tamano?: number;
}

export function buscarCitas(filtro: FiltroCitas, signal?: AbortSignal): Promise<PaginaCitas> {
  return api<PaginaCitas>('/api/citas', {
    parametros: {
      desde: filtro.desde,
      hasta: filtro.hasta,
      estado: filtro.estado,
      paciente_id: filtro.pacienteId,
      recientes: filtro.recientes,
      pagina: filtro.pagina,
      tamano: filtro.tamano,
    },
    signal,
  });
}

export function obtenerCita(id: number, signal?: AbortSignal): Promise<Cita> {
  return api<Cita>(`/api/citas/${id}`, { signal });
}

export interface PeticionDeHuecos {
  tratamientoId: number;
  fecha: string;
  odontologoId: number | null;
  /** Al mover una cita, su propio hueco cuenta como libre. */
  excluirCitaId?: number;
}

/** Huecos libres de un día. Los calcula la API: aquí no se decide si una hora está libre. */
export function buscarHuecos(peticion: PeticionDeHuecos, signal?: AbortSignal): Promise<Hueco[]> {
  return api<Hueco[]>('/api/agenda/disponibilidad', {
    parametros: {
      tratamiento_id: peticion.tratamientoId,
      fecha: peticion.fecha,
      odontologo_id: peticion.odontologoId,
      excluir_cita_id: peticion.excluirCitaId,
    },
    signal,
  });
}

export function darCita(pacienteId: number, datos: NuevaCita): Promise<Cita> {
  return api<Cita>(`/api/pacientes/${pacienteId}/citas`, { metodo: 'POST', cuerpo: datos });
}

export function cambiarEstadoDeCita(id: number, estado: EstadoCita): Promise<Cita> {
  return api<Cita>(`/api/citas/${id}/estado`, { metodo: 'PUT', cuerpo: { estado } });
}

/** No se borra: queda CANCELADA y su hueco libre (también en la web). */
export function cancelarCita(id: number, motivo: string): Promise<Cita> {
  return api<Cita>(`/api/citas/${id}/cancelacion`, {
    metodo: 'POST',
    cuerpo: { motivo: motivo.trim() || undefined },
  });
}

/** Devuelve la cita nueva; la anterior queda REPROGRAMADA. */
export function reprogramarCita(id: number, datos: Reprogramacion): Promise<Cita> {
  return api<Cita>(`/api/citas/${id}/reprogramacion`, { metodo: 'POST', cuerpo: datos });
}

export function vincularPaciente(id: number, pacienteId: number): Promise<Cita> {
  return api<Cita>(`/api/citas/${id}/paciente`, { metodo: 'PUT', cuerpo: { paciente_id: pacienteId } });
}

export function cambiarNotas(id: number, notas: string): Promise<Cita> {
  return api<Cita>(`/api/citas/${id}/notas`, {
    metodo: 'PUT',
    cuerpo: { notas_internas: notas.trim() || undefined },
  });
}

export function listarTratamientos(signal?: AbortSignal): Promise<Tratamiento[]> {
  return api<Tratamiento[]>('/api/tratamientos', { signal });
}

export function listarOdontologos(signal?: AbortSignal): Promise<Odontologo[]> {
  return api<Odontologo[]>('/api/odontologos', { signal });
}
