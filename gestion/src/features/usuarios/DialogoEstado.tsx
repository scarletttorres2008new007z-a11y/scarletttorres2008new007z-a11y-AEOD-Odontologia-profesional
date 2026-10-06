import { useMutation } from '@tanstack/react-query';
import { mensajeDeError } from '../../shared/api/errores';
import type { Usuario } from '../../shared/api/tipos';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Dialogo } from '../../shared/ui/Dialogo';
import { cambiarEstadoDelUsuario } from './api';
import { useRefrescarUsuarios, type PropsDialogo } from './comun';

export type CambioDeEstado = 'desactivar' | 'activar' | 'desbloquear';

const TEXTOS: Record<CambioDeEstado, { titulo: string; explicacion: string; boton: string; hecho: string }> =
  {
    desactivar: {
      titulo: '¿Desactivar el usuario de {nombre}?',
      explicacion:
        'No podrá entrar al software y sus sesiones abiertas se cerrarán al momento. Sus datos y su historial se conservan, y puedes volver a activarlo cuando quieras.',
      boton: 'Desactivar',
      hecho: 'Se ha desactivado el usuario de {nombre} y se han cerrado sus sesiones.',
    },
    activar: {
      titulo: '¿Activar el usuario de {nombre}?',
      explicacion: 'Podrá volver a entrar con su contraseña de siempre.',
      boton: 'Activar',
      hecho: 'Se ha activado el usuario de {nombre}.',
    },
    desbloquear: {
      titulo: '¿Desbloquear el usuario de {nombre}?',
      explicacion:
        'Se bloqueó durante unos minutos por fallar la contraseña varias veces seguidas. Si lo desbloqueas, podrá volver a intentarlo ahora.',
      boton: 'Desbloquear',
      hecho: 'Se ha desbloqueado el usuario de {nombre}.',
    },
  };

export function DialogoEstado({
  usuario,
  cambio,
  alCerrar,
  alTerminar,
}: PropsDialogo & { usuario: Usuario; cambio: CambioDeEstado }) {
  const refrescar = useRefrescarUsuarios();
  const textos = TEXTOS[cambio];
  const conNombre = (texto: string) => texto.replace('{nombre}', usuario.nombre);

  const guardar = useMutation({
    mutationFn: () => cambiarEstadoDelUsuario(usuario.id, cambio !== 'desactivar'),
    onSuccess: async () => {
      await refrescar();
      alTerminar(conNombre(textos.hecho));
    },
  });

  return (
    <Dialogo
      titulo={conNombre(textos.titulo)}
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton
            type="submit"
            variante={cambio === 'desactivar' ? 'peligro' : 'primario'}
            cargando={guardar.isPending}
          >
            {textos.boton}
          </Boton>
        </>
      }
    >
      <p>{textos.explicacion}</p>
      {guardar.error && <Aviso tipo="error">{mensajeDeError(guardar.error)}</Aviso>}
    </Dialogo>
  );
}
