import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { CitaResumen, Paciente, PaginaCitas } from '../../shared/api/tipos';
import { conMayuscula, formatearDia, hoyEnMadrid, sumarDias } from '../../shared/fechas';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { Paginacion } from '../../shared/ui/Paginacion';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { buscarCitas } from './api';
import { CitaAbierta } from './CitaAbierta';
import { ESTADOS } from './comun';
import { DialogoNuevaCita } from './DialogoNuevaCita';
import css from './Agenda.module.css';

/** Las citas de un paciente en su ficha: las de hoy en adelante y su historial. */
export function CitasDelPaciente({ paciente }: { paciente: Paciente }) {
  const { tienePermiso } = useAuth();
  const idTitulo = useId();
  const hoy = hoyEnMadrid();
  const [pagina, setPagina] = useState(0);
  const [abierta, setAbierta] = useState<number | null>(null);
  const [avisoCita, setAvisoCita] = useState('');
  const [darAbierto, setDarAbierto] = useState(false);
  const [hecho, setHecho] = useState('');

  const proximas = useQuery({
    queryKey: ['citas', 'paciente', paciente.id, 'proximas', hoy],
    queryFn: ({ signal }) => buscarCitas({ pacienteId: paciente.id, desde: hoy, tamano: 50 }, signal),
  });
  const anteriores = useQuery({
    queryKey: ['citas', 'paciente', paciente.id, 'anteriores', hoy, pagina],
    queryFn: ({ signal }) =>
      buscarCitas(
        { pacienteId: paciente.id, hasta: sumarDias(hoy, -1), recientes: true, pagina, tamano: 10 },
        signal,
      ),
    placeholderData: keepPreviousData,
  });
  const puedeDarCita = paciente.activo && tienePermiso(PERMISOS.CITAS_CREAR);

  return (
    <section className={p.tarjeta} aria-labelledby={idTitulo}>
      <div className={css.cabeceraSeccion}>
        <h2 id={idTitulo}>Citas</h2>
        {puedeDarCita && (
          <Boton
            pequeno
            onClick={() => {
              setHecho('');
              setDarAbierto(true);
            }}
          >
            Dar cita
          </Boton>
        )}
      </div>
      {hecho && <Aviso tipo="exito">{hecho}</Aviso>}

      <h3 className={css.subtitulo}>Hoy y próximas</h3>
      <ListaDeCitas
        consulta={proximas}
        vacia="No tiene citas a partir de hoy."
        resumen="Citas de hoy en adelante, de la más cercana a la más lejana"
        alAbrir={setAbierta}
      />

      <h3 className={css.subtitulo}>Anteriores</h3>
      <ListaDeCitas
        consulta={anteriores}
        vacia="Todavía no tiene citas anteriores."
        resumen="Citas anteriores, de la más reciente a la más antigua"
        alAbrir={setAbierta}
      />
      {anteriores.data && anteriores.data.total_paginas > 1 && (
        <Paginacion
          pagina={anteriores.data.pagina}
          totalPaginas={anteriores.data.total_paginas}
          totalElementos={anteriores.data.total_elementos}
          alCambiar={setPagina}
        />
      )}

      {darAbierto && (
        <DialogoNuevaCita
          paciente={paciente}
          fecha={hoy}
          alCerrar={() => setDarAbierto(false)}
          alTerminar={(_cita, mensaje) => {
            setDarAbierto(false);
            setHecho(mensaje);
          }}
        />
      )}
      {abierta && (
        <CitaAbierta
          key={abierta}
          id={abierta}
          aviso={avisoCita}
          alCerrar={() => {
            setAvisoCita('');
            setAbierta(null);
          }}
          alAbrir={(id) => {
            setAvisoCita('');
            setAbierta(id);
          }}
          alReprogramar={(nueva, mensaje) => {
            setAvisoCita(mensaje);
            setAbierta(nueva.id);
          }}
        />
      )}
    </section>
  );
}

interface PropsLista {
  consulta: { data?: PaginaCitas; error: Error | null; isPending: boolean };
  vacia: string;
  resumen: string;
  alAbrir: (id: number) => void;
}

function ListaDeCitas({ consulta, vacia, resumen, alAbrir }: PropsLista) {
  if (consulta.error) return <Aviso tipo="error">{mensajeDeError(consulta.error)}</Aviso>;
  if (consulta.isPending || !consulta.data) return <Cargando texto="Cargando citas…" />;
  const citas = consulta.data.contenido;
  if (citas.length === 0) return <p className={css.nota}>{vacia}</p>;
  return (
    <div className={p.tablaCaja}>
      <table className={p.tabla}>
        <caption className="visually-hidden">{resumen}</caption>
        <thead>
          <tr>
            <th scope="col">Día y hora</th>
            <th scope="col">Tratamiento</th>
            <th scope="col">Odontólogo</th>
            <th scope="col">Estado</th>
            <th scope="col">
              <span className="visually-hidden">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {citas.map((cita) => (
            <FilaDeCita key={cita.id} cita={cita} alAbrir={alAbrir} />
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FilaDeCita({ cita, alAbrir }: { cita: CitaResumen; alAbrir: (id: number) => void }) {
  return (
    <tr>
      <td className={css.sinCorte}>
        {conMayuscula(formatearDia(cita.fecha, true))}
        <span className={p.secundario}>
          {cita.hora_inicio} a {cita.hora_fin}
        </span>
      </td>
      <td>{cita.tratamiento}</td>
      <td className={css.sinCorte}>{cita.odontologo}</td>
      <td>
        <Insignia tono={ESTADOS[cita.estado].tono}>{ESTADOS[cita.estado].texto}</Insignia>
      </td>
      <td>
        <Boton variante="texto" pequeno onClick={() => alAbrir(cita.id)}>
          Ver<TextoOculto>la cita del {formatearDia(cita.fecha)}</TextoOculto>
        </Boton>
      </td>
    </tr>
  );
}
