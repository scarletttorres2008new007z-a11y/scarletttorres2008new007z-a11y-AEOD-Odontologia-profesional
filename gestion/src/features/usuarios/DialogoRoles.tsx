import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeDeError, mensajeGeneral } from '../../shared/api/errores';
import type { Usuario } from '../../shared/api/tipos';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Dialogo } from '../../shared/ui/Dialogo';
import { useAuth } from '../auth/contexto';
import { CLAVE_ROLES, listarRoles } from '../roles/api';
import { cambiarRolesDelUsuario } from './api';
import { ADMINISTRADOR, rolesBloqueados, useRefrescarUsuarios, type PropsDialogo } from './comun';
import { SelectorDeRoles } from './SelectorDeRoles';

export function DialogoRoles({ usuario, alCerrar, alTerminar }: PropsDialogo & { usuario: Usuario }) {
  const { usuario: yo } = useAuth();
  const refrescar = useRefrescarUsuarios();
  const soyAdministrador = yo?.roles.some((rol) => rol.codigo === ADMINISTRADOR) ?? false;
  const [elegidos, setElegidos] = useState(() => usuario.roles.map((rol) => rol.codigo));
  const roles = useQuery({ queryKey: CLAVE_ROLES, queryFn: listarRoles });

  const guardar = useMutation({
    mutationFn: () => cambiarRolesDelUsuario(usuario.id, elegidos),
    onSuccess: async (guardado) => {
      await refrescar();
      alTerminar(`Se han guardado los roles de ${guardado.nombre}. Sus permisos cambian desde ya.`);
    },
  });

  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, ['roles']);

  return (
    <Dialogo
      titulo={`Roles de ${usuario.nombre}`}
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending} disabled={!roles.data}>
            Guardar roles
          </Boton>
        </>
      }
    >
      <p>Lo que puede hacer en el software depende de sus roles. Puede tener varios a la vez.</p>
      {roles.data ? (
        <SelectorDeRoles
          roles={roles.data}
          elegidos={elegidos}
          alCambiar={setElegidos}
          bloqueados={rolesBloqueados(soyAdministrador, usuario, yo ?? undefined)}
          error={errores.roles}
        />
      ) : roles.error ? (
        <Aviso tipo="error">{mensajeDeError(roles.error)}</Aviso>
      ) : (
        <Cargando texto="Cargando los roles…" />
      )}
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
