import { useMutation, useQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { erroresDeCampos, mensajeDeError, mensajeGeneral } from '../../shared/api/errores';
import type { DatosOdontologo, OdontologoConfigurado } from '../../shared/api/tipos';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { AreaDeTexto, Campo, Selector } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { CLAVES, crearOdontologo, editarOdontologo, listarUsuariosParaVincular } from './api';
import { useRefrescarConfiguracion, type PropsDialogo } from './comun';

const CAMPOS = ['nombre', 'especialidad', 'descripcion', 'usuario_id'] as const;

/**
 * Alta de un odontólogo (sin `odontologo`) o cambio de sus datos y de su usuario del software. La API comprueba
 * que el nombre no se repite y que el usuario no es ya de otro odontólogo.
 */
export function DialogoOdontologo({
  odontologo,
  alCerrar,
  alTerminar,
  mensajeAlCrear,
}: PropsDialogo & {
  odontologo?: OdontologoConfigurado;
  mensajeAlCrear?: (creado: OdontologoConfigurado) => ReactNode;
}) {
  const refrescar = useRefrescarConfiguracion();
  const crear = odontologo === undefined;
  const actual = odontologo?.usuario;

  const [nombre, setNombre] = useState(odontologo?.nombre ?? '');
  const [especialidad, setEspecialidad] = useState(odontologo?.especialidad ?? '');
  const [descripcion, setDescripcion] = useState(odontologo?.descripcion ?? '');
  const [usuarioId, setUsuarioId] = useState(actual ? String(actual.id) : '');

  const usuarios = useQuery({
    queryKey: CLAVES.usuarios,
    queryFn: ({ signal }) => listarUsuariosParaVincular(signal),
  });

  const guardar = useMutation({
    mutationFn: () => {
      const datos: DatosOdontologo = {
        nombre: nombre.trim(),
        especialidad: especialidad.trim() || undefined,
        descripcion: descripcion.trim() || undefined,
        usuario_id: usuarioId ? Number(usuarioId) : undefined,
      };
      return odontologo ? editarOdontologo(odontologo.id, datos) : crearOdontologo(datos);
    },
    onSuccess: async (guardado) => {
      await refrescar();
      if (crear) {
        alTerminar({ mensaje: mensajeAlCrear?.(guardado) ?? `Se ha dado de alta a ${guardado.nombre}.` });
        return;
      }
      const vinculado = guardado.usuario && guardado.usuario.id !== actual?.id ? guardado.usuario : null;
      alTerminar({
        mensaje: vinculado
          ? `Se han guardado los datos de ${guardado.nombre}. Cuando ${vinculado.nombre} entre al software verá su agenda.`
          : `Se han guardado los datos de ${guardado.nombre}.`,
      });
    },
  });

  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, CAMPOS);
  // Si su usuario se desactivó, la API ya no lo ofrece: se enseña igualmente para no quitárselo sin querer
  const opciones = usuarios.data ?? [];
  const actualFueraDeLista = actual && usuarios.data && !opciones.some((u) => u.id === actual.id);

  return (
    <Dialogo
      titulo={crear ? 'Nuevo odontólogo' : `Editar a ${odontologo.nombre}`}
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardar.isPending}>
            {crear ? 'Dar de alta' : 'Guardar cambios'}
          </Boton>
        </>
      }
    >
      <Campo
        etiqueta="Nombre"
        required
        autoComplete="off"
        maxLength={120}
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
        ayuda="Tal como saldrá en la web y en la agenda, por ejemplo «Dra. Ana Villar»."
        error={errores.nombre}
      />
      <Campo
        etiqueta="Especialidad"
        autoComplete="off"
        maxLength={150}
        value={especialidad}
        onChange={(e) => setEspecialidad(e.target.value)}
        ayuda="Sale en la web, debajo del nombre."
        error={errores.especialidad}
      />
      <AreaDeTexto
        etiqueta="Presentación"
        rows={3}
        maxLength={1000}
        value={descripcion}
        onChange={(e) => setDescripcion(e.target.value)}
        ayuda="Unas líneas sobre su experiencia. Salen en la web, en el equipo."
        error={errores.descripcion}
      />
      {usuarios.error ? (
        <Aviso tipo="error">{mensajeDeError(usuarios.error)}</Aviso>
      ) : (
        <Selector
          etiqueta="Usuario del software"
          value={usuarioId}
          disabled={usuarios.isPending}
          onChange={(e) => setUsuarioId(e.target.value)}
          ayuda="Con su usuario, al entrar al software ve su propia agenda. Si aún no tiene, se crea en «Usuarios»."
          error={errores.usuario_id}
        >
          <option value="">{usuarios.isPending ? 'Cargando usuarios…' : 'Sin usuario'}</option>
          {actualFueraDeLista && (
            <option
              value={String(actual.id)}
            >{`${actual.nombre} (${actual.username}) · usuario desactivado`}</option>
          )}
          {opciones.map((u) => {
            const deOtro = u.odontologo_id !== undefined && u.odontologo_id !== odontologo?.id;
            const roles = u.roles.length > 0 ? ` · ${u.roles.join(', ')}` : '';
            const deQuien = deOtro ? ` · ya es de ${u.odontologo}` : '';
            return (
              <option key={u.id} value={String(u.id)} disabled={deOtro}>
                {`${u.nombre} (${u.username})${roles}${deQuien}`}
              </option>
            );
          })}
        </Selector>
      )}
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
