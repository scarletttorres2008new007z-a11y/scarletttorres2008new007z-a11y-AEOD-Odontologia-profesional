import { useMutation } from '@tanstack/react-query';
import { mensajeDeError } from '../../shared/api/errores';
import type { Paciente } from '../../shared/api/tipos';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Dialogo } from '../../shared/ui/Dialogo';
import { cambiarEstadoDelPaciente } from './api';
import { nombreCompleto, useRefrescarPacientes, type PropsDialogo } from './comun';

/** Dar de baja (si está activo) o reactivar (si está de baja). */
export function DialogoEstadoPaciente({
  paciente,
  alCerrar,
  alTerminar,
}: PropsDialogo & { paciente: Paciente }) {
  const refrescar = useRefrescarPacientes();
  const nombre = nombreCompleto(paciente);
  const baja = paciente.activo;

  const guardar = useMutation({
    mutationFn: () => cambiarEstadoDelPaciente(paciente.id, !baja),
    onSuccess: async (guardado) => {
      await refrescar(guardado.id);
      alTerminar(guardado, baja ? `Se ha dado de baja a ${nombre}.` : `Se ha reactivado a ${nombre}.`);
    },
  });

  return (
    <Dialogo
      titulo={baja ? `¿Dar de baja a ${nombre}?` : `¿Reactivar a ${nombre}?`}
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" variante={baja ? 'peligro' : 'primario'} cargando={guardar.isPending}>
            {baja ? 'Dar de baja' : 'Reactivar'}
          </Boton>
        </>
      }
    >
      <p>
        {baja
          ? 'Dejará de figurar como paciente activo. No se borra nada: sus datos y su historial se conservan y puedes reactivarlo cuando quieras.'
          : 'Volverá a figurar como paciente activo, con todos sus datos.'}
      </p>
      {guardar.error && <Aviso tipo="error">{mensajeDeError(guardar.error)}</Aviso>}
    </Dialogo>
  );
}
