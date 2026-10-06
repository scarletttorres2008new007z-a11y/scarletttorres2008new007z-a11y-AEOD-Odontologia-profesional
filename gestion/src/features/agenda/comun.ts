import { useQuery, useQueryClient } from '@tanstack/react-query';
import type {
  Cita,
  CitaResumen,
  EstadoCita,
  OrigenCita,
  Odontologo,
  TipoDeBloqueo,
  Tratamiento,
} from '../../shared/api/tipos';
import { formatearDia } from '../../shared/fechas';
import type { Tono } from '../../shared/ui/Insignia';
import { listarOdontologos, listarTratamientos } from './api';

// Textos en castellano para los códigos de las citas. Qué se puede hacer con cada cita lo dice la API
// (estados_siguientes y modificable); aquí solo se le pone nombre.

export const ESTADOS: Record<EstadoCita, { texto: string; tono: Tono }> = {
  PENDIENTE: { texto: 'Por confirmar', tono: 'alerta' },
  CONFIRMADA: { texto: 'Confirmada', tono: 'marca' },
  EN_ATENCION: { texto: 'En consulta', tono: 'exito' },
  COMPLETADA: { texto: 'Completada', tono: 'neutro' },
  NO_ASISTIO: { texto: 'No asistió', tono: 'error' },
  CANCELADA: { texto: 'Cancelada', tono: 'neutro' },
  REPROGRAMADA: { texto: 'Reprogramada', tono: 'neutro' },
};

/** Estados que se ven en la agenda (las canceladas y las movidas dejan su hueco libre). */
export const ESTADOS_EN_AGENDA: EstadoCita[] = [
  'PENDIENTE',
  'CONFIRMADA',
  'EN_ATENCION',
  'COMPLETADA',
  'NO_ASISTIO',
];

/** «la web», «la clínica»: para frases como «Reservada en la web». */
export const ORIGENES: Record<OrigenCita, string> = {
  LANDING: 'la web',
  SOFTWARE: 'la clínica',
  APP: 'la app de pacientes',
};

export const BLOQUEOS: Record<TipoDeBloqueo, string> = {
  ALMUERZO: 'Almuerzo',
  REUNION: 'Reunión',
  MANTENIMIENTO: 'Mantenimiento',
  VACACIONES: 'Vacaciones',
  FERIADO: 'Festivo',
  MANUAL: 'No disponible',
};

/** Texto del botón que pasa la cita al estado `nuevo`. */
export function textoDelCambio(actual: EstadoCita, nuevo: EstadoCita): string {
  switch (nuevo) {
    case 'CONFIRMADA':
      return actual === 'NO_ASISTIO' ? 'Sí vino: deshacer «No asistió»' : 'Confirmar';
    case 'EN_ATENCION':
      return 'Pasa a consulta';
    case 'COMPLETADA':
      return 'Consulta terminada';
    case 'NO_ASISTIO':
      return 'No ha venido';
    default:
      return ESTADOS[nuevo].texto;
  }
}

/** Mensaje que confirma el cambio de estado. */
export function cambioHecho(estado: EstadoCita): string {
  switch (estado) {
    case 'CONFIRMADA':
      return 'Cita confirmada.';
    case 'EN_ATENCION':
      return 'El paciente está en consulta.';
    case 'COMPLETADA':
      return 'Consulta terminada.';
    case 'NO_ASISTIO':
      return 'Cita marcada como «No asistió».';
    default:
      return `La cita está ahora ${ESTADOS[estado].texto.toLowerCase()}.`;
  }
}

/** «lunes 12 de octubre a las 10:00 con Dra. Ana Villar» */
export function cuando(cita: Pick<Cita, 'fecha' | 'hora_inicio' | 'odontologo'>): string {
  return `${formatearDia(cita.fecha)} a las ${cita.hora_inicio} con ${cita.odontologo}`;
}

/** El nombre del paciente: el de su ficha o, si aún no tiene, el que escribió en la web. */
export function nombreDeCita(cita: Cita): string {
  return cita.paciente?.nombre ?? cita.contacto.nombre;
}

/** Enlace a una cita dentro de la agenda (se abre encima del día en que está). */
export function enlaceDeCita(cita: Pick<CitaResumen, 'id' | 'fecha'>): string {
  return `/agenda?fecha=${cita.fecha}&cita=${cita.id}`;
}

/** Tratamientos y odontólogos activos (los mismos que ofrece la web). Cambian poco: se guardan un rato. */
export function useTratamientos() {
  return useQuery({
    queryKey: ['tratamientos'],
    queryFn: ({ signal }) => listarTratamientos(signal),
    staleTime: 5 * 60_000,
  });
}

export function useOdontologos() {
  return useQuery({
    queryKey: ['odontologos'],
    queryFn: ({ signal }) => listarOdontologos(signal),
    staleTime: 5 * 60_000,
  });
}

/**
 * Odontólogos que la API indica para ese tratamiento (si no indica ninguno, lo hace cualquiera).
 * Es solo para no ofrecer opciones imposibles: la API vuelve a comprobarlo al guardar.
 */
export function quienesLoHacen(
  tratamiento: Tratamiento | undefined,
  odontologos: Odontologo[],
): Odontologo[] {
  if (!tratamiento || tratamiento.odontologo_ids.length === 0) return odontologos;
  return odontologos.filter((odontologo) => tratamiento.odontologo_ids.includes(odontologo.id));
}

/** Tras cambiar una cita: la agenda, las listas, las citas abiertas y los huecos libres. */
export function useRefrescarCitas(): () => Promise<unknown> {
  const queryClient = useQueryClient();
  return () =>
    Promise.all(
      ['agenda', 'citas', 'cita', 'huecos'].map((clave) =>
        queryClient.invalidateQueries({ queryKey: [clave] }),
      ),
    );
}
