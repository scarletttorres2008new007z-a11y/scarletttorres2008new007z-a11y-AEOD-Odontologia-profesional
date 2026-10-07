import { useQueryClient } from '@tanstack/react-query';
import type { Paciente, Sexo, TipoDocumento } from '../../shared/api/tipos';

export const TIPOS_DE_DOCUMENTO: Record<TipoDocumento, string> = {
  DNI: 'DNI',
  NIE: 'NIE',
  PASAPORTE: 'Pasaporte',
  OTRO: 'Otro',
};

export const AYUDA_DEL_DOCUMENTO: Record<TipoDocumento, string> = {
  DNI: '8 números y una letra, por ejemplo 12345678Z.',
  NIE: 'X, Y o Z, 7 números y una letra, por ejemplo X1234567L.',
  PASAPORTE: 'Tal y como aparece en el pasaporte.',
  OTRO: 'Entre 3 y 20 letras o números.',
};

export const SEXOS: Record<Sexo, string> = {
  MUJER: 'Mujer',
  HOMBRE: 'Hombre',
  OTRO: 'Otro',
};

/** Props comunes de los diálogos de pacientes. */
export interface PropsDialogo {
  alCerrar: () => void;
  /** Se llama al guardar, con el paciente guardado y el mensaje que confirma lo que se ha hecho. */
  alTerminar: (paciente: Paciente, mensaje: string) => void;
}

export function nombreCompleto(paciente: Pick<Paciente, 'nombres' | 'apellidos'>): string {
  return `${paciente.nombres} ${paciente.apellidos}`;
}

/** "DNI 12345678Z", o vacío si no tiene. */
export function documento(paciente: Paciente): string {
  if (!paciente.tipo_documento || !paciente.numero_documento) return '';
  return `${TIPOS_DE_DOCUMENTO[paciente.tipo_documento]} ${paciente.numero_documento}`;
}

export function textoDeEdad(edad: number | undefined): string {
  if (edad === undefined) return '';
  return edad === 1 ? '1 año' : `${edad} años`;
}

/** Tras cambiar un paciente: la lista y su ficha. */
export function useRefrescarPacientes(): (id: number) => Promise<unknown> {
  const queryClient = useQueryClient();
  return (id) =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['pacientes'] }),
      queryClient.invalidateQueries({ queryKey: ['paciente', id] }),
    ]);
}
