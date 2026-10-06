import { useQuery } from '@tanstack/react-query';
import { useId, useState, type ReactNode } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { Paciente } from '../../shared/api/tipos';
import { formatearTelefono } from '../../shared/formato';
import { useValorEstable } from '../../shared/hooks';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo } from '../../shared/ui/Campo';
import { buscarPacientes } from '../pacientes/api';
import { documento, nombreCompleto } from '../pacientes/comun';
import css from './Agenda.module.css';

const RESULTADOS = 6;

interface Props {
  elegido: Paciente | null;
  alElegir: (paciente: Paciente | null) => void;
  /** Texto con el que empieza la búsqueda (por ejemplo, el teléfono que dejó en la web). */
  textoInicial?: string;
  ayuda?: ReactNode;
  error?: string;
}

/** Busca un paciente activo y lo deja elegido; «Cambiar» vuelve a la búsqueda. */
export function ElegirPaciente({ elegido, alElegir, textoInicial = '', ayuda, error }: Props) {
  const [texto, setTexto] = useState(textoInicial);
  const busqueda = useValorEstable(texto.trim());
  const idResultados = useId();
  const buscar = busqueda.length >= 2 && !elegido;
  const { data, error: errorBusqueda } = useQuery({
    queryKey: ['pacientes', 'buscador', busqueda],
    queryFn: ({ signal }) =>
      buscarPacientes({ texto: busqueda, activo: 'true', pagina: 0 }, signal, RESULTADOS),
    enabled: buscar,
  });

  if (elegido) {
    return (
      <div className={css.pacienteElegido}>
        <div>
          <p className={css.pacienteNombre}>{nombreCompleto(elegido)}</p>
          <p className={css.pacienteDatos}>
            Código {elegido.codigo} · {formatearTelefono(elegido.telefono)}
            {documento(elegido) && ` · ${documento(elegido)}`}
          </p>
        </div>
        <Boton variante="texto" pequeno onClick={() => alElegir(null)}>
          Cambiar<span className="visually-hidden"> de paciente</span>
        </Boton>
      </div>
    );
  }

  return (
    <div className={css.buscador}>
      <Campo
        etiqueta="Buscar paciente"
        type="search"
        autoComplete="off"
        placeholder="Nombre, apellidos, DNI, teléfono o código"
        value={texto}
        onChange={(evento) => setTexto(evento.target.value)}
        ayuda={ayuda}
        error={error}
        aria-controls={idResultados}
      />
      <div id={idResultados} aria-live="polite">
        {errorBusqueda ? (
          <Aviso tipo="error">{mensajeDeError(errorBusqueda)}</Aviso>
        ) : !buscar ? null : !data ? (
          <p className={css.nota}>Buscando…</p>
        ) : data.contenido.length === 0 ? (
          <p className={css.nota}>No hay pacientes activos que coincidan con «{busqueda}».</p>
        ) : (
          <>
            <ul className={css.resultados} aria-label="Pacientes encontrados">
              {data.contenido.map((paciente) => (
                <li key={paciente.id}>
                  <button type="button" className={css.resultado} onClick={() => alElegir(paciente)}>
                    <span className={css.pacienteNombre}>
                      {paciente.apellidos}, {paciente.nombres}
                    </span>
                    <span className={css.pacienteDatos}>
                      Código {paciente.codigo} · {formatearTelefono(paciente.telefono)}
                      {documento(paciente) && ` · ${documento(paciente)}`}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {data.total_elementos > data.contenido.length && (
              <p className={css.nota}>Hay más pacientes: escribe algo más para afinar la búsqueda.</p>
            )}
          </>
        )}
      </div>
    </div>
  );
}
