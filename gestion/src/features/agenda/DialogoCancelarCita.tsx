import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import type { Cita } from '../../shared/api/tipos';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { cancelarCita } from './api';
import { cuando, nombreDeCita } from './comun';

interface Props {
  cita: Cita;
  alCerrar: () => void;
  alTerminar: (cita: Cita, mensaje: string) => void;
}

/** Cancelar no borra la cita: queda en el historial como cancelada y su hueco se libera, también en la web. */
export function DialogoCancelarCita({ cita, alCerrar, alTerminar }: Props) {
  const [motivo, setMotivo] = useState('');
  const guardar = useMutation({
    mutationFn: () => cancelarCita(cita.id, motivo),
    onSuccess: (cancelada) =>
      alTerminar(
        cancelada,
        'Cita cancelada. Su hueco vuelve a estar libre, también para reservar en la web.',
      ),
  });
  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, ['motivo']);

  return (
    <Dialogo
      titulo="¿Cancelar esta cita?"
      alCerrar={alCerrar}
      alEnviar={() => guardar.mutate()}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Volver
          </Boton>
          <Boton type="submit" variante="peligro" cargando={guardar.isPending}>
            Cancelar la cita
          </Boton>
        </>
      }
    >
      <p>
        <strong>{nombreDeCita(cita)}</strong>: {cita.tratamiento}, el {cuando(cita)}.
      </p>
      <p>
        No se borra: queda en el historial como cancelada y el hueco vuelve a estar libre, también para
        reservar en la web.
      </p>
      <Campo
        etiqueta="Motivo"
        maxLength={255}
        autoComplete="off"
        value={motivo}
        onChange={(evento) => setMotivo(evento.target.value)}
        ayuda="Opcional. Por ejemplo: avisó por teléfono de que no puede venir."
        error={errores.motivo}
      />
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
