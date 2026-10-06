import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { Cita, Paciente } from '../../shared/api/tipos';
import { formatearTelefono } from '../../shared/formato';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Dialogo } from '../../shared/ui/Dialogo';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { nombreCompleto } from '../pacientes/comun';
import { DialogoPaciente, type DatosIniciales } from '../pacientes/DialogoPaciente';
import { vincularPaciente } from './api';
import { ElegirPaciente } from './ElegirPaciente';
import css from './Agenda.module.css';

interface Props {
  cita: Cita;
  alCerrar: () => void;
  alTerminar: (cita: Cita, mensaje: string) => void;
}

/**
 * Una reserva de la web llega con los datos que escribió la persona. Recepción la vincula a la ficha del paciente
 * (o le da de alta con esos datos) para que la cita quede en su historial.
 */
export function DialogoVincular({ cita, alCerrar, alTerminar }: Props) {
  const { tienePermiso } = useAuth();
  const [paciente, setPaciente] = useState<Paciente | null>(null);
  const [alta, setAlta] = useState(false);
  const [falta, setFalta] = useState('');
  const contacto = cita.contacto;

  const vincular = useMutation({
    mutationFn: ({ elegido }: { elegido: Paciente; mensaje: string }) =>
      vincularPaciente(cita.id, elegido.id),
    onSuccess: (vinculada, { mensaje }) => alTerminar(vinculada, mensaje),
  });

  if (alta) {
    return (
      <DialogoPaciente
        inicial={datosDeLaWeb(cita)}
        alCerrar={() => setAlta(false)}
        alTerminar={(nuevo) => {
          setAlta(false);
          setPaciente(nuevo);
          vincular.mutate({
            elegido: nuevo,
            mensaje: `Se ha dado de alta a ${nombreCompleto(nuevo)} (código ${nuevo.codigo}) y la cita ha quedado en su ficha.`,
          });
        }}
      />
    );
  }

  const enviar = () => {
    setFalta(paciente ? '' : 'Elige el paciente o dalo de alta con estos datos.');
    if (paciente) {
      vincular.mutate({
        elegido: paciente,
        mensaje: `La cita ha quedado en la ficha de ${nombreCompleto(paciente)}.`,
      });
    }
  };

  return (
    <Dialogo
      titulo="Vincular la cita a un paciente"
      alCerrar={alCerrar}
      alEnviar={enviar}
      pie={
        <>
          <Boton variante="secundario" onClick={alCerrar}>
            Volver
          </Boton>
          <Boton type="submit" cargando={vincular.isPending}>
            Vincular
          </Boton>
        </>
      }
    >
      <div className={css.datosWeb}>
        <p className={css.nota}>Datos que dejó al reservar en la web</p>
        <p>
          <strong>{contacto.nombre}</strong> · {formatearTelefono(contacto.telefono)}
          {contacto.email && ` · ${contacto.email}`}
        </p>
      </div>
      <ElegirPaciente
        elegido={paciente}
        alElegir={(elegido) => {
          setPaciente(elegido);
          setFalta('');
        }}
        textoInicial={ultimosDigitos(contacto.telefono)}
        ayuda="Empezamos buscando por su teléfono. También puedes escribir su nombre, DNI o código."
        error={falta}
      />
      {!paciente && tienePermiso(PERMISOS.PACIENTES_CREAR) && (
        <p className={css.nota}>
          ¿Aún no tiene ficha?{' '}
          <Boton variante="texto" pequeno onClick={() => setAlta(true)}>
            Darle de alta con estos datos
          </Boton>
        </p>
      )}
      {vincular.error && <Aviso tipo="error">{mensajeDeError(vincular.error)}</Aviso>}
    </Dialogo>
  );
}

/** Los 9 últimos dígitos: encuentran el teléfono de la ficha tenga o no el prefijo +34. */
function ultimosDigitos(telefono: string): string {
  return telefono.replace(/\D/g, '').slice(-9);
}

/** En la web se escribe el nombre completo en un solo campo: la primera palabra va al nombre y el resto a los apellidos. */
function datosDeLaWeb(cita: Cita): DatosIniciales {
  const [nombres = '', ...apellidos] = cita.contacto.nombre.trim().split(/\s+/);
  return {
    nombres,
    apellidos: apellidos.join(' '),
    telefono: cita.contacto.telefono,
    email: cita.contacto.email ?? '',
  };
}
