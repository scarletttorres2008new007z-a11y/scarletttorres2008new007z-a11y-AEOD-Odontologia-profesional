import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../../shared/api/errores';
import type { Paciente } from '../../shared/api/tipos';
import { formatearTelefono } from '../../shared/formato';
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
import { buscarPacientes, type FiltroPacientes } from './api';
import { documento, nombreCompleto, textoDeEdad } from './comun';
import { DialogoPaciente } from './DialogoPaciente';
import css from './Pacientes.module.css';

type Accion = { tipo: 'crear' } | { tipo: 'editar'; paciente: Paciente };

/** Lo que se ha hecho, con un enlace a la ficha del paciente. */
interface Hecho {
  mensaje: string;
  paciente: Paciente;
}

export function PaginaPacientes() {
  useTitulo('Pacientes');
  const { tienePermiso } = useAuth();
  const [texto, setTexto] = useState('');
  const [activo, setActivo] = useState<FiltroPacientes['activo']>('');
  const [pagina, setPagina] = useState(0);
  const [accion, setAccion] = useState<Accion | null>(null);
  const [hecho, setHecho] = useState<Hecho | null>(null);

  const filtro: FiltroPacientes = { texto: useValorEstable(texto.trim()), activo, pagina };
  const { data, error, isPending, isPlaceholderData } = useQuery({
    queryKey: ['pacientes', filtro],
    queryFn: ({ signal }) => buscarPacientes(filtro, signal),
    placeholderData: keepPreviousData,
  });

  const puedeCrear = tienePermiso(PERMISOS.PACIENTES_CREAR);
  const puedeEditar = tienePermiso(PERMISOS.PACIENTES_EDITAR);
  const hayFiltros = filtro.texto !== '' || activo !== '';

  const abrir = (nueva: Accion) => {
    setHecho(null);
    setAccion(nueva);
  };
  const terminar = (paciente: Paciente, mensaje: string) => {
    setAccion(null);
    setHecho({ paciente, mensaje });
  };

  return (
    <div className={p.pagina}>
      <div className={p.cabecera}>
        <div>
          <h1>Pacientes</h1>
          <p className={p.entradilla}>
            Datos personales y de contacto de los pacientes. Lo clínico irá en su expediente.
          </p>
        </div>
        {puedeCrear && (
          <div className={p.acciones}>
            <Boton onClick={() => abrir({ tipo: 'crear' })}>Nuevo paciente</Boton>
          </div>
        )}
      </div>

      <div className={p.filtros} role="search">
        <Campo
          etiqueta="Buscar"
          type="search"
          placeholder="Nombre, apellidos, DNI, teléfono, correo o código"
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
            setActivo(e.target.value as FiltroPacientes['activo']);
            setPagina(0);
          }}
        >
          <option value="">Todos</option>
          <option value="true">Activos</option>
          <option value="false">De baja</option>
        </Selector>
      </div>

      {hecho && (
        <Aviso tipo="exito">
          {hecho.mensaje} <Link to={`/pacientes/${hecho.paciente.id}`}>Ver su ficha</Link>
        </Aviso>
      )}
      {error && <Aviso tipo="error">{mensajeDeError(error)}</Aviso>}

      {isPending && <Cargando texto="Cargando pacientes…" />}
      {data && data.contenido.length === 0 && (
        <Vacio>
          {hayFiltros
            ? 'No hay pacientes que coincidan con la búsqueda.'
            : puedeCrear
              ? 'Todavía no hay pacientes. Da de alta el primero con «Nuevo paciente».'
              : 'Todavía no hay pacientes.'}
        </Vacio>
      )}
      {data && data.contenido.length > 0 && (
        <div>
          <div className={p.tablaCaja} aria-busy={isPlaceholderData}>
            <table className={p.tabla}>
              <caption className="visually-hidden">Pacientes, por orden alfabético de apellidos</caption>
              <thead>
                <tr>
                  <th scope="col">Paciente</th>
                  <th scope="col">Documento</th>
                  <th scope="col">Teléfono</th>
                  <th scope="col">Edad</th>
                  <th scope="col">Estado</th>
                  {puedeEditar && (
                    <th scope="col">
                      <span className="visually-hidden">Acciones</span>
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {data.contenido.map((paciente) => (
                  <tr key={paciente.id}>
                    <td className={css.celdaNombre}>
                      <Link to={`/pacientes/${paciente.id}`} className={css.nombre}>
                        {paciente.apellidos}, {paciente.nombres}
                      </Link>
                      <span className={p.secundario}>Código {paciente.codigo}</span>
                    </td>
                    <td className={css.sinCorte}>
                      {documento(paciente) || <span className={p.secundario}>Sin documento</span>}
                    </td>
                    <td className={css.sinCorte}>{formatearTelefono(paciente.telefono)}</td>
                    <td className={css.sinCorte}>{textoDeEdad(paciente.edad) || '—'}</td>
                    <td>
                      {paciente.activo ? (
                        <Insignia tono="exito">Activo</Insignia>
                      ) : (
                        <Insignia>De baja</Insignia>
                      )}
                    </td>
                    {puedeEditar && (
                      <td>
                        <Boton variante="texto" pequeno onClick={() => abrir({ tipo: 'editar', paciente })}>
                          Editar<TextoOculto>a {nombreCompleto(paciente)}</TextoOculto>
                        </Boton>
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

      {accion?.tipo === 'crear' && <DialogoPaciente alCerrar={() => setAccion(null)} alTerminar={terminar} />}
      {accion?.tipo === 'editar' && (
        <DialogoPaciente paciente={accion.paciente} alCerrar={() => setAccion(null)} alTerminar={terminar} />
      )}
    </div>
  );
}
