import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeDeError, mensajeGeneral } from '../../shared/api/errores';
import type { Usuario } from '../../shared/api/tipos';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo, CampoPassword } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { CLAVE_ROLES, listarRoles } from '../roles/api';
import { crearUsuario, editarUsuario } from './api';
import { ADMINISTRADOR, rolesBloqueados, useRefrescarUsuarios, type PropsDialogo } from './comun';
import { SelectorDeRoles } from './SelectorDeRoles';

const CAMPOS = ['nombre', 'username', 'email', 'password', 'roles'] as const;

/** Alta de un usuario (sin `usuario`) o edición de su nombre, usuario y correo. */
export function DialogoUsuario({ usuario, alCerrar, alTerminar }: PropsDialogo & { usuario?: Usuario }) {
  const { usuario: yo, tienePermiso } = useAuth();
  const refrescar = useRefrescarUsuarios();
  const crear = usuario === undefined;
  const puedeDarRoles = crear && tienePermiso(PERMISOS.USUARIOS_ASIGNAR_ROLES);
  const soyAdministrador = yo?.roles.some((rol) => rol.codigo === ADMINISTRADOR) ?? false;

  const [nombre, setNombre] = useState(usuario?.nombre ?? '');
  const [username, setUsername] = useState(usuario?.username ?? '');
  const [email, setEmail] = useState(usuario?.email ?? '');
  const [password, setPassword] = useState('');
  const [roles, setRoles] = useState<string[]>([]);

  const rolesDisponibles = useQuery({ queryKey: CLAVE_ROLES, queryFn: listarRoles, enabled: puedeDarRoles });

  const guardar = useMutation({
    mutationFn: () => {
      const datos = { nombre: nombre.trim(), username: username.trim(), email: email.trim() };
      return usuario ? editarUsuario(usuario.id, datos) : crearUsuario({ ...datos, password, roles });
    },
    onSuccess: async (guardado) => {
      await refrescar();
      alTerminar(
        crear
          ? `Se ha creado el usuario «${guardado.username}». Ya puede entrar con la contraseña que le has puesto.`
          : `Se han guardado los datos de ${guardado.nombre}.`,
      );
    },
  });

  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, CAMPOS);

  return (
    <Dialogo
      titulo={crear ? 'Nuevo usuario' : `Editar a ${usuario.nombre}`}
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            {crear ? 'Crear usuario' : 'Guardar cambios'}
          </Boton>
        </>
      }
    >
      <Campo
        etiqueta="Nombre y apellidos"
        required
        autoComplete="off"
        maxLength={100}
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
        error={errores.nombre}
      />
      <Campo
        etiqueta="Usuario"
        required
        autoComplete="off"
        autoCapitalize="off"
        spellCheck={false}
        maxLength={50}
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        ayuda="Con él se entra al software. Letras sin tildes, números, punto, guion o guion bajo."
        error={errores.username}
      />
      <Campo
        etiqueta="Correo"
        type="email"
        required
        autoComplete="off"
        maxLength={150}
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        ayuda="También sirve para entrar, en lugar del usuario."
        error={errores.email}
      />
      {crear && (
        <CampoPassword
          etiqueta="Contraseña inicial"
          required
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          ayuda="Mínimo 10 caracteres, distinta del usuario y del correo. Dísela en persona y pídele que la cambie en «Mi cuenta»."
          error={errores.password}
        />
      )}
      {puedeDarRoles &&
        (rolesDisponibles.data ? (
          <SelectorDeRoles
            roles={rolesDisponibles.data}
            elegidos={roles}
            alCambiar={setRoles}
            bloqueados={rolesBloqueados(soyAdministrador)}
            error={errores.roles}
          />
        ) : rolesDisponibles.error ? (
          <Aviso tipo="error">{mensajeDeError(rolesDisponibles.error)}</Aviso>
        ) : (
          <Cargando texto="Cargando los roles…" />
        ))}
      {crear && !puedeDarRoles && (
        <Aviso tipo="info">
          Se creará sin roles. Se los puede dar alguien con permiso para asignar roles.
        </Aviso>
      )}
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
