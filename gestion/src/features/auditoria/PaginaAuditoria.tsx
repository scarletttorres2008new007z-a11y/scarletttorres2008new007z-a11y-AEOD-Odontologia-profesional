import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { RegistroAuditoria } from '../../shared/api/tipos';
import { formatearFechaHora } from '../../shared/formato';
import { useTitulo, useValorEstable } from '../../shared/hooks';
import { Aviso, Cargando, Vacio } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo, Selector } from '../../shared/ui/Campo';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { Paginacion } from '../../shared/ui/Paginacion';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import { buscarEnAuditoria, type FiltroAuditoria } from './api';
import { DetalleAuditoria } from './DetalleAuditoria';
import { ACCIONES, afectado, autor, ENTIDADES_EN_PLURAL, GRUPOS_DE_ACCIONES, ORIGENES } from './etiquetas';

type Filtros = Omit<FiltroAuditoria, 'pagina'>;

const SIN_FILTROS: Filtros = { entidad: '', accion: '', entidadId: '', desde: '', hasta: '' };

export function PaginaAuditoria() {
  useTitulo('Auditoría');
  const [filtros, setFiltros] = useState<Filtros>(SIN_FILTROS);
  const [pagina, setPagina] = useState(0);
  const [detalle, setDetalle] = useState<RegistroAuditoria | null>(null);

  const consulta: FiltroAuditoria = {
    ...filtros,
    entidadId: useValorEstable(filtros.entidadId.trim()),
    pagina,
  };
  const { data, error, isPending, isPlaceholderData } = useQuery({
    queryKey: ['auditoria', consulta],
    queryFn: ({ signal }) => buscarEnAuditoria(consulta, signal),
    placeholderData: keepPreviousData,
  });

  const cambiar = <K extends keyof Filtros>(clave: K, valor: Filtros[K]) => {
    setFiltros((antes) => ({ ...antes, [clave]: valor }));
    setPagina(0);
  };
  const hayFiltros = Object.values(filtros).some((valor) => valor !== '');

  return (
    <div className={p.pagina}>
      <div>
        <h1>Auditoría</h1>
        <p className={p.entradilla}>
          Registro de las operaciones importantes: quién hizo qué, cuándo y desde dónde. Solo se puede
          consultar.
        </p>
      </div>

      <div className={p.filtros} role="search">
        <Selector
          etiqueta="Afecta a"
          value={filtros.entidad}
          onChange={(e) => cambiar('entidad', e.target.value as Filtros['entidad'])}
        >
          <option value="">Todo</option>
          {Object.entries(ENTIDADES_EN_PLURAL).map(([codigo, texto]) => (
            <option key={codigo} value={codigo}>
              {texto}
            </option>
          ))}
        </Selector>
        <Selector
          etiqueta="Acción"
          value={filtros.accion}
          onChange={(e) => cambiar('accion', e.target.value as Filtros['accion'])}
        >
          <option value="">Todas</option>
          {GRUPOS_DE_ACCIONES.map((grupo) => (
            <optgroup key={grupo.titulo} label={grupo.titulo}>
              {grupo.acciones.map((accion) => (
                <option key={accion} value={accion}>
                  {ACCIONES[accion]}
                </option>
              ))}
            </optgroup>
          ))}
        </Selector>
        <Campo
          etiqueta="N.º del registro"
          inputMode="numeric"
          placeholder="Ej.: 12"
          value={filtros.entidadId}
          onChange={(e) => cambiar('entidadId', e.target.value)}
        />
        <Campo
          etiqueta="Desde el día"
          type="date"
          value={filtros.desde}
          max={filtros.hasta || undefined}
          onChange={(e) => cambiar('desde', e.target.value)}
        />
        <Campo
          etiqueta="Hasta el día"
          type="date"
          value={filtros.hasta}
          min={filtros.desde || undefined}
          onChange={(e) => cambiar('hasta', e.target.value)}
        />
        {hayFiltros && (
          <Boton
            variante="texto"
            onClick={() => {
              setFiltros(SIN_FILTROS);
              setPagina(0);
            }}
          >
            Quitar filtros
          </Boton>
        )}
      </div>

      {error && <Aviso tipo="error">{mensajeDeError(error)}</Aviso>}
      {isPending && <Cargando texto="Cargando la auditoría…" />}
      {data && data.contenido.length === 0 && (
        <Vacio>
          {hayFiltros ? 'No hay operaciones con esos filtros.' : 'Todavía no hay operaciones registradas.'}
        </Vacio>
      )}
      {data && data.contenido.length > 0 && (
        <div>
          <div className={p.tablaCaja} aria-busy={isPlaceholderData}>
            <table className={p.tabla}>
              <caption className="visually-hidden">
                Operaciones registradas, de la más reciente a la más antigua
              </caption>
              <thead>
                <tr>
                  <th scope="col">Fecha y hora</th>
                  <th scope="col">Quién</th>
                  <th scope="col">Acción</th>
                  <th scope="col">Afecta a</th>
                  <th scope="col">Desde</th>
                  <th scope="col">
                    <span className="visually-hidden">Detalle</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.contenido.map((registro) => {
                  const quien = autor(registro);
                  return (
                    <tr key={registro.id}>
                      <td>{formatearFechaHora(registro.creado_en, true)}</td>
                      <td>
                        {quien.nombre}
                        {quien.detalle && <span className={p.secundario}>{quien.detalle}</span>}
                      </td>
                      <td>{ACCIONES[registro.accion]}</td>
                      <td>{afectado(registro)}</td>
                      <td>
                        <Insignia tono={registro.origen === 'SOFTWARE' ? 'neutro' : 'marca'}>
                          {ORIGENES[registro.origen]}
                        </Insignia>
                      </td>
                      <td>
                        <Boton variante="texto" pequeno onClick={() => setDetalle(registro)}>
                          Ver
                          <TextoOculto>el detalle de la operación {registro.id}</TextoOculto>
                        </Boton>
                      </td>
                    </tr>
                  );
                })}
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

      {detalle && <DetalleAuditoria registro={detalle} alCerrar={() => setDetalle(null)} />}
    </div>
  );
}
