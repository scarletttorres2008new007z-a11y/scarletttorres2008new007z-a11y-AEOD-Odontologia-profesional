import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { Usuario } from '../../shared/api/tipos';
import { formatearFechaHora } from '../../shared/formato';
import { useTitulo, useValorEstable } from '../../shared/hooks';
import { Aviso, Cargando, Vacio } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo, Selector } from '../../shared/ui/Campo';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { Paginacion } from '../../shared/ui/Paginacion';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { buscarUsuarios, type FiltroUsuarios } from './api';
import { DialogoEstado, type CambioDeEstado } from './DialogoEstado';
import { DialogoPassword } from './DialogoPassword';
import { DialogoRoles } from './DialogoRoles';
import { DialogoUsuario } from './DialogoUsuario';

type Accion =
  | { tipo: 'crear' }
  | { tipo: 'editar' | 'roles' | 'password'; usuario: Usuario }
  | { tipo: 'estado'; usuario: Usuario; cambio: CambioDeEstado };

export function PaginaUsuarios() {
  useTitulo('Usuarios');
  const { usuario: yo, tienePermiso } = useAuth();
  const [texto, setTexto] = useState('');
  const [activo, setActivo] = useState<FiltroUsuarios['activo']>('');
  const [pagina, setPagina] = useState(0);
  const [accion, setAccion] = useState<Accion | null>(null);
  const [hecho, setHecho] = useState('');

  const filtro: FiltroUsuarios = { texto: useValorEstable(texto.trim()), activo, pagina };
  const { data, error, isPending, isPlaceholderData } = useQuery({
    queryKey: ['usuarios', filtro],
    queryFn: ({ signal }) => buscarUsuarios(filtro, signal),
    placeholderData: keepPreviousData,
  });

  const puedeCrear = tienePermiso(PERMISOS.USUARIOS_CREAR);
  const puedeEditar = tienePermiso(PERMISOS.USUARIOS_EDITAR);
  const puedeDarRoles = tienePermiso(PERMISOS.USUARIOS_ASIGNAR_ROLES);
  const conAcciones = puedeEditar || puedeDarRoles;

  const abrir = (nueva: Accion) => {
    setHecho('');
    setAccion(nueva);
  };
  const cerrar = () => setAccion(null);
  const terminar = (mensaje: string) => {
    setAccion(null);
    setHecho(mensaje);
  };

  const acciones = (u: Usuario): ReactNode[] => {
    const esYo = u.id === yo?.id;
    const quien = <TextoOculto>a {u.nombre}</TextoOculto>;
    const botones: ReactNode[] = [];
    if (puedeEditar) {
      botones.push(
        <Boton key="editar" variante="texto" pequeno onClick={() => abrir({ tipo: 'editar', usuario: u })}>
          Editar{quien}
        </Boton>,
      );
    }
    if (puedeDarRoles) {
      botones.push(
        <Boton key="roles" variante="texto" pequeno onClick={() => abrir({ tipo: 'roles', usuario: u })}>
          Roles<TextoOculto>de {u.nombre}</TextoOculto>
        </Boton>,
      );
    }
    if (puedeEditar && !esYo) {
      botones.push(
        <Boton
          key="password"
          variante="texto"
          pequeno
          onClick={() => abrir({ tipo: 'password', usuario: u })}
        >
          Contraseña<TextoOculto>de {u.nombre}</TextoOculto>
        </Boton>,
      );
    }
    if (puedeEditar && u.activo && u.bloqueado) {
      botones.push(
        <Boton
          key="desbloquear"
          variante="texto"
          pequeno
          onClick={() => abrir({ tipo: 'estado', usuario: u, cambio: 'desbloquear' })}
        >
          Desbloquear{quien}
        </Boton>,
      );
    }
    if (puedeEditar && !esYo) {
      const cambio: CambioDeEstado = u.activo ? 'desactivar' : 'activar';
      botones.push(
        <Boton
          key="estado"
          variante="texto"
          pequeno
          onClick={() => abrir({ tipo: 'estado', usuario: u, cambio })}
        >
          {u.activo ? 'Desactivar' : 'Activar'}
          {quien}
        </Boton>,
      );
    }
    return botones;
  };

  const hayFiltros = filtro.texto !== '' || activo !== '';

  return (
    <div className={p.pagina}>
      <div className={p.cabecera}>
        <div>
          <h1>Usuarios</h1>
          <p className={p.entradilla}>
            Personal con acceso al software. Cada persona entra con su propio usuario.
          </p>
        </div>
        {puedeCrear && (
          <div className={p.acciones}>
            <Boton onClick={() => abrir({ tipo: 'crear' })}>Nuevo usuario</Boton>
          </div>
        )}
      </div>

      <div className={p.filtros} role="search">
        <Campo
          etiqueta="Buscar"
          type="search"
          placeholder="Nombre, usuario o correo"
          value={texto}
          onChange={(e) => {
            setTexto(e.target.value);
            setPagina(0);
          }}
        />
        <Selector
          etiqueta="Estado"
          value={activo}
          onChange={(e) => {
            setActivo(e.target.value as FiltroUsuarios['activo']);
            setPagina(0);
          }}
        >
          <option value="">Todos</option>
          <option value="true">Activos</option>
          <option value="false">Desactivados</option>
        </Selector>
      </div>

      {hecho && <Aviso tipo="exito">{hecho}</Aviso>}
      {error && <Aviso tipo="error">{mensajeDeError(error)}</Aviso>}

      {isPending && <Cargando texto="Cargando usuarios…" />}
      {data && data.contenido.length === 0 && (
        <Vacio>
          {hayFiltros ? 'No hay usuarios que coincidan con la búsqueda.' : 'Todavía no hay usuarios.'}
        </Vacio>
      )}
      {data && data.contenido.length > 0 && (
        <div>
          <div className={p.tablaCaja} aria-busy={isPlaceholderData}>
            <table className={p.tabla}>
              <caption className="visually-hidden">Usuarios del software</caption>
              <thead>
                <tr>
                  <th scope="col">Nombre</th>
                  <th scope="col">Correo</th>
                  <th scope="col">Roles</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Último acceso</th>
                  {conAcciones && (
                    <th scope="col">
                      <span className="visually-hidden">Acciones</span>
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {data.contenido.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <strong>{u.nombre}</strong> {u.id === yo?.id && <Insignia tono="marca">Tú</Insignia>}
                      <span className={p.secundario}>{u.username}</span>
                    </td>
                    <td>{u.email}</td>
                    <td>
                      {u.roles.length > 0 ? (
                        <span className={p.lista}>
                          {u.roles.map((rol) => (
                            <Insignia key={rol.codigo}>{rol.nombre}</Insignia>
                          ))}
                        </span>
                      ) : (
                        <span className={p.secundario}>Sin rol</span>
                      )}
                    </td>
                    <td>
                      <EstadoDelUsuario usuario={u} />
                    </td>
                    <td>{u.ultimo_acceso ? formatearFechaHora(u.ultimo_acceso) : 'Nunca'}</td>
                    {conAcciones && (
                      <td>
                        <div className={p.celdaAcciones}>{acciones(u)}</div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginacion
            pagina={data.pagina}
            totalPaginas={data.total_paginas}
            totalElementos={data.total_elementos}
            alCambiar={setPagina}
          />
        </div>
      )}

      {accion?.tipo === 'crear' && <DialogoUsuario alCerrar={cerrar} alTerminar={terminar} />}
      {accion?.tipo === 'editar' && (
        <DialogoUsuario usuario={accion.usuario} alCerrar={cerrar} alTerminar={terminar} />
      )}
      {accion?.tipo === 'roles' && (
        <DialogoRoles usuario={accion.usuario} alCerrar={cerrar} alTerminar={terminar} />
      )}
      {accion?.tipo === 'password' && (
        <DialogoPassword usuario={accion.usuario} alCerrar={cerrar} alTerminar={terminar} />
      )}
      {accion?.tipo === 'estado' && (
        <DialogoEstado
          usuario={accion.usuario}
          cambio={accion.cambio}
          alCerrar={cerrar}
          alTerminar={terminar}
        />
      )}
    </div>
  );
}

function EstadoDelUsuario({ usuario }: { usuario: Usuario }) {
  if (!usuario.activo) return <Insignia>Desactivado</Insignia>;
  if (usuario.bloqueado) {
    const hora = formatearFechaHora(usuario.bloqueado_hasta).slice(-5);
    return <Insignia tono="alerta">Bloqueado hasta las {hora}</Insignia>;
  }
  return <Insignia tono="exito">Activo</Insignia>;
}
