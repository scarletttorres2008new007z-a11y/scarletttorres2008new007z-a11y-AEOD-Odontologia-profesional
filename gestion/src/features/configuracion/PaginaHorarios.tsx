import { useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { Link } from 'react-router';
import { mensajeDeError } from '../../shared/api/errores';
import type { DiaDeApertura, TurnosDeOdontologo } from '../../shared/api/tipos';
import { conMayuscula, minutosDe } from '../../shared/fechas';
import { useTitulo } from '../../shared/hooks';
import { Aviso, Cargando, Vacio } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import p from '../../shared/ui/pagina.module.css';
import { TextoOculto } from '../../shared/ui/TextoOculto';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { CLAVES, obtenerHorarios } from './api';
import { AvisoDeResultado } from './AvisoDeResultado';
import { DIAS, tramo, type Resultado } from './comun';
import css from './Configuracion.module.css';
import { DialogoHorarioClinica } from './DialogoHorarioClinica';
import { DialogoTurnos } from './DialogoTurnos';

type Accion = { tipo: 'clinica' } | { tipo: 'turnos'; odontologo: TurnosDeOdontologo };

interface FilaDeDia {
  numero: number;
  texto: string;
  /** El día no abre o no trabaja: se ve atenuado. */
  libre: boolean;
  nota?: string;
}

export function PaginaHorarios() {
  useTitulo('Horarios');
  const { tienePermiso } = useAuth();
  const idClinica = useId();
  const idTurnos = useId();
  const horarios = useQuery({ queryKey: CLAVES.horarios, queryFn: ({ signal }) => obtenerHorarios(signal) });
  const [accion, setAccion] = useState<Accion | null>(null);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  const abrir = (nueva: Accion) => {
    setResultado(null);
    setAccion(nueva);
  };
  const cerrar = () => setAccion(null);
  const terminar = (hecho: Resultado) => {
    setAccion(null);
    setResultado(hecho);
  };

  const clinica = horarios.data?.clinica ?? [];
  const aperturaDel = (dia: number) => clinica.find((d) => d.dia_semana === dia);

  return (
    <div className={p.pagina}>
      <div>
        <h1>Horarios</h1>
        <p className={p.entradilla}>
          Una cita solo se puede dar dentro del horario de la clínica y de los turnos de su odontólogo. Los
          cambios se notan al momento en la web y en la agenda.
        </p>
      </div>

      {resultado && <AvisoDeResultado resultado={resultado} />}
      {horarios.error && <Aviso tipo="error">{mensajeDeError(horarios.error)}</Aviso>}
      {horarios.isPending && <Cargando texto="Cargando horarios…" />}
      {horarios.data && (
        <>
          <section className={`${p.tarjeta} ${css.horario}`} aria-labelledby={idClinica}>
            <div className={css.cabeceraTarjeta}>
              <h2 id={idClinica}>Horario de la clínica</h2>
              <Boton variante="secundario" pequeno onClick={() => abrir({ tipo: 'clinica' })}>
                Cambiar<TextoOculto>el horario de la clínica</TextoOculto>
              </Boton>
            </div>
            <Semana
              filas={DIAS.map((dia) => {
                const apertura = aperturaDel(dia.numero);
                return {
                  numero: dia.numero,
                  texto: apertura ? tramo(apertura.hora_apertura, apertura.hora_cierre) : 'Cerrado',
                  libre: !apertura,
                };
              })}
            />
            <p className={p.secundario}>
              El almuerzo y otros cortes que se repiten cada semana están en{' '}
              {tienePermiso(PERMISOS.BLOQUEOS_GESTIONAR) ? (
                <Link to="/bloqueos">Bloqueos</Link>
              ) : (
                '«Bloqueos»'
              )}
              .
            </p>
          </section>

          <section className={css.seccion} aria-labelledby={idTurnos}>
            <h2 id={idTurnos}>Turnos de cada odontólogo</h2>
            {horarios.data.odontologos.length === 0 ? (
              <Vacio>No hay odontólogos activos. Se dan de alta en «Odontólogos».</Vacio>
            ) : (
              <div className={css.rejilla}>
                {horarios.data.odontologos.map((odontologo) => (
                  <TarjetaDeTurnos
                    key={odontologo.id}
                    odontologo={odontologo}
                    clinica={clinica}
                    alCambiar={() => abrir({ tipo: 'turnos', odontologo })}
                  />
                ))}
              </div>
            )}
          </section>
        </>
      )}

      {accion?.tipo === 'clinica' && horarios.data && (
        <DialogoHorarioClinica clinica={clinica} alCerrar={cerrar} alTerminar={terminar} />
      )}
      {accion?.tipo === 'turnos' && (
        <DialogoTurnos
          odontologo={accion.odontologo}
          clinica={clinica}
          alCerrar={cerrar}
          alTerminar={terminar}
        />
      )}
    </div>
  );
}

function TarjetaDeTurnos({
  odontologo,
  clinica,
  alCambiar,
}: {
  odontologo: TurnosDeOdontologo;
  clinica: DiaDeApertura[];
  alCambiar: () => void;
}) {
  const idTitulo = useId();
  const filas = DIAS.map((dia): FilaDeDia => {
    const turnos = odontologo.turnos.filter((t) => t.dia_semana === dia.numero);
    const apertura = clinica.find((d) => d.dia_semana === dia.numero);
    // Solo es una ayuda para verlo: los huecos los calcula siempre la API con los dos horarios
    const fuera = turnos.some(
      (t) =>
        !apertura ||
        minutosDe(t.hora_inicio) < minutosDe(apertura.hora_apertura) ||
        minutosDe(t.hora_fin) > minutosDe(apertura.hora_cierre),
    );
    return {
      numero: dia.numero,
      texto:
        turnos.length > 0 ? turnos.map((t) => tramo(t.hora_inicio, t.hora_fin)).join(' y ') : 'No trabaja',
      libre: turnos.length === 0,
      nota: !fuera
        ? undefined
        : apertura
          ? `Solo cuenta de ${tramo(apertura.hora_apertura, apertura.hora_cierre)}, cuando abre la clínica`
          : 'La clínica no abre ese día',
    };
  });

  return (
    <section className={`${p.tarjeta} ${css.horario}`} aria-labelledby={idTitulo}>
      <div className={css.cabeceraTarjeta}>
        <h3 id={idTitulo}>{odontologo.nombre}</h3>
        <Boton variante="secundario" pequeno onClick={alCambiar}>
          Cambiar turnos<TextoOculto>de {odontologo.nombre}</TextoOculto>
        </Boton>
      </div>
      <Semana filas={filas} />
    </section>
  );
}

function Semana({ filas }: { filas: FilaDeDia[] }) {
  return (
    <dl className={css.semana}>
      {filas.map((fila) => (
        <div key={fila.numero} className={css.dia} data-libre={fila.libre || undefined}>
          <dt>{conMayuscula(DIAS[fila.numero - 1]?.nombre ?? '')}</dt>
          <dd>
            {fila.texto}
            {fila.nota && <span className={css.nota}>{fila.nota}</span>}
          </dd>
        </div>
      ))}
    </dl>
  );
}
