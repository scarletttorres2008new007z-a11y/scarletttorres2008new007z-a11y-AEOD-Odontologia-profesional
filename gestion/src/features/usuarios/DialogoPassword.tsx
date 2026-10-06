import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import type { Usuario } from '../../shared/api/tipos';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { CampoPassword } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { restablecerPassword } from './api';
import { useRefrescarUsuarios, type PropsDialogo } from './comun';

/** Contraseña nueva para otra persona (la propia se cambia en «Mi cuenta»). */
export function DialogoPassword({ usuario, alCerrar, alTerminar }: PropsDialogo & { usuario: Usuario }) {
  const refrescar = useRefrescarUsuarios();
  const [password, setPassword] = useState('');

  const guardar = useMutation({
    mutationFn: () => restablecerPassword(usuario.id, password),
    onSuccess: async () => {
      await refrescar();
      alTerminar(`Se ha cambiado la contraseña de ${usuario.nombre} y se han cerrado sus sesiones abiertas.`);
    },
  });

  const general = mensajeGeneral(guardar.error, ['password']);

  return (
    <Dialogo
      titulo={`Nueva contraseña para ${usuario.nombre}`}
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            Cambiar contraseña
          </Boton>
        </>
      }
    >
      <p>
        Sus sesiones abiertas se cerrarán y tendrá que entrar con la contraseña nueva. Si su usuario estaba
        bloqueado por intentos fallidos, se desbloquea.
      </p>
      <CampoPassword
        etiqueta="Contraseña nueva"
        required
        autoComplete="new-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        ayuda="Mínimo 10 caracteres, distinta de su usuario y de su correo. Dísela en persona y pídele que la cambie en «Mi cuenta»."
        error={erroresDeCampos(guardar.error).password}
      />
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
