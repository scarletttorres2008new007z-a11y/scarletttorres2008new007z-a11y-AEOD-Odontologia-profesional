import { useId } from 'react';
import type { CitaResumen } from '../../shared/api/tipos';
import { Aviso } from '../../shared/ui/Aviso';
import p from '../../shared/ui/pagina.module.css';
import { ListaDeCitas } from '../agenda/FilaDeCita';
import type { Resultado } from './comun';
import css from './Configuracion.module.css';

/**
 * Lo que se acaba de guardar y, si el cambio deja citas pendientes o confirmadas fuera de horario o dentro de un
 * bloqueo, cuáles son. Esas citas no se cancelan solas: la clínica decide si moverlas o cancelarlas.
 */
export function AvisoDeResultado({ resultado }: { resultado: Resultado }) {
  const { mensaje, afectadas } = resultado;
  return (
    <>
      {/* En un solo bloque: el aviso es flexible y separaría el texto de sus enlaces */}
      <Aviso tipo="exito">
        <span>{mensaje}</span>
      </Aviso>
      {afectadas && afectadas.citas.total > 0 && (
        <CitasPorRevisar
          total={afectadas.citas.total}
          citas={afectadas.citas.citas}
          motivo={afectadas.motivo}
        />
      )}
    </>
  );
}

function CitasPorRevisar({
  total,
  citas,
  motivo,
}: {
  total: number;
  citas: CitaResumen[] | undefined;
  motivo: string;
}) {
  const idTitulo = useId();
  const una = total === 1;
  return (
    <section className={`${p.tarjeta} ${css.revisar}`} aria-labelledby={idTitulo}>
      <h2 id={idTitulo}>{una ? '1 cita por revisar' : `${total} citas por revisar`}</h2>
      <p>
        {una
          ? `Hay 1 cita pendiente o confirmada que ${motivo}. No se ha cancelado: muévela o cancélala desde la agenda si hace falta.`
          : `Hay ${total} citas pendientes o confirmadas que ${motivo}. No se ha cancelado ninguna: muévelas o cancélalas desde la agenda si hace falta.`}
      </p>
      {citas ? (
        <>
          <ListaDeCitas citas={citas} />
          {total > citas.length && <p className={p.secundario}>Y {total - citas.length} más.</p>}
        </>
      ) : (
        <p className={p.secundario}>
          Tu usuario no puede ver las citas de todas las agendas: avisa a recepción para que las revise.
        </p>
      )}
    </section>
  );
}
