import { useMutation } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import type { DatosPaciente, Paciente, Sexo, TipoDocumento } from '../../shared/api/tipos';
import { formatearTelefono } from '../../shared/formato';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { AreaDeTexto, Campo, Selector } from '../../shared/ui/Campo';
import { Dialogo } from '../../shared/ui/Dialogo';
import { crearPaciente, editarPaciente } from './api';
import {
  AYUDA_DEL_DOCUMENTO,
  nombreCompleto,
  SEXOS,
  TIPOS_DE_DOCUMENTO,
  useRefrescarPacientes,
  type PropsDialogo,
} from './comun';
import css from './Pacientes.module.css';

const CAMPOS = [
  'nombres',
  'apellidos',
  'tipo_documento',
  'numero_documento',
  'fecha_nacimiento',
  'sexo',
  'telefono',
  'email',
  'direccion',
  'contacto_emergencia_nombre',
  'contacto_emergencia_telefono',
  'observaciones',
] as const;

type Formulario = Record<(typeof CAMPOS)[number], string>;

function formularioDe(paciente?: Paciente): Formulario {
  return {
    nombres: paciente?.nombres ?? '',
    apellidos: paciente?.apellidos ?? '',
    tipo_documento: paciente?.tipo_documento ?? '',
    numero_documento: paciente?.numero_documento ?? '',
    fecha_nacimiento: paciente?.fecha_nacimiento ?? '',
    sexo: paciente?.sexo ?? '',
    telefono: formatearTelefono(paciente?.telefono),
    email: paciente?.email ?? '',
    direccion: paciente?.direccion ?? '',
    contacto_emergencia_nombre: paciente?.contacto_emergencia_nombre ?? '',
    contacto_emergencia_telefono: formatearTelefono(paciente?.contacto_emergencia_telefono),
    observaciones: paciente?.observaciones ?? '',
  };
}

function opcional(valor: string): string | undefined {
  return valor.trim() || undefined;
}

/** Lo que se envía a la API. Los campos vacíos no se envían: la API los guarda vacíos. */
function datosDe(formulario: Formulario): DatosPaciente {
  return {
    nombres: formulario.nombres.trim(),
    apellidos: formulario.apellidos.trim(),
    tipo_documento: (formulario.tipo_documento || undefined) as TipoDocumento | undefined,
    numero_documento: opcional(formulario.numero_documento),
    fecha_nacimiento: formulario.fecha_nacimiento || undefined,
    sexo: (formulario.sexo || undefined) as Sexo | undefined,
    telefono: formulario.telefono.trim(),
    email: opcional(formulario.email),
    direccion: opcional(formulario.direccion),
    contacto_emergencia_nombre: opcional(formulario.contacto_emergencia_nombre),
    contacto_emergencia_telefono: opcional(formulario.contacto_emergencia_telefono),
    observaciones: opcional(formulario.observaciones),
  };
}

/** Hoy en el formato de los campos de fecha (aaaa-mm-dd), para no dejar elegir un nacimiento futuro. */
function hoy(): string {
  const ahora = new Date();
  const mes = String(ahora.getMonth() + 1).padStart(2, '0');
  const dia = String(ahora.getDate()).padStart(2, '0');
  return `${ahora.getFullYear()}-${mes}-${dia}`;
}

/** Datos con los que empieza un alta (por ejemplo, los que dejó en la web al reservar). */
export type DatosIniciales = Partial<Pick<Formulario, 'nombres' | 'apellidos' | 'telefono' | 'email'>>;

/** Alta de un paciente (sin `paciente`) o edición de sus datos. */
export function DialogoPaciente({
  paciente,
  inicial,
  alCerrar,
  alTerminar,
}: PropsDialogo & { paciente?: Paciente; inicial?: DatosIniciales }) {
  const refrescar = useRefrescarPacientes();
  const crear = paciente === undefined;
  const [formulario, setFormulario] = useState<Formulario>(() => ({ ...formularioDe(paciente), ...inicial }));
  const campos = useRef<HTMLDivElement>(null);

  const guardar = useMutation({
    mutationFn: () => {
      const datos = datosDe(formulario);
      return paciente ? editarPaciente(paciente.id, datos) : crearPaciente(datos);
    },
    onSuccess: async (guardado) => {
      await refrescar(guardado.id);
      alTerminar(
        guardado,
        crear
          ? `Se ha dado de alta a ${nombreCompleto(guardado)}. Su código de paciente es ${guardado.codigo}.`
          : `Se han guardado los datos de ${nombreCompleto(guardado)}.`,
      );
    },
  });

  // El formulario es largo: tras un error, el foco va al primer dato que hay que corregir, aunque no se vea
  const error = guardar.error;
  useEffect(() => {
    if (error) campos.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  }, [error]);

  const errores = erroresDeCampos(guardar.error);
  const general = mensajeGeneral(guardar.error, CAMPOS);
  const tipo = formulario.tipo_documento as TipoDocumento | '';

  const props = (campo: keyof Formulario) => ({
    value: formulario[campo],
    onChange: (evento: { target: { value: string } }) =>
      setFormulario((antes) => ({ ...antes, [campo]: evento.target.value })),
    error: errores[campo],
  });

  return (
    <Dialogo
      titulo={crear ? 'Nuevo paciente' : `Editar a ${nombreCompleto(paciente)}`}
      ancho="amplio"
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
      <div ref={campos} className={css.formulario}>
        <fieldset className={css.bloque}>
          <legend>Datos personales</legend>
          <div className={css.columnas}>
            <Campo etiqueta="Nombre" required autoComplete="off" maxLength={100} {...props('nombres')} />
            <Campo etiqueta="Apellidos" required autoComplete="off" maxLength={100} {...props('apellidos')} />
            <Selector etiqueta="Documento" {...props('tipo_documento')}>
              <option value="">Sin documento</option>
              {Object.entries(TIPOS_DE_DOCUMENTO).map(([codigo, texto]) => (
                <option key={codigo} value={codigo}>
                  {texto}
                </option>
              ))}
            </Selector>
            <Campo
              etiqueta="Número de documento"
              autoComplete="off"
              autoCapitalize="characters"
              spellCheck={false}
              maxLength={30}
              ayuda={tipo ? AYUDA_DEL_DOCUMENTO[tipo] : 'Elige antes el tipo de documento.'}
              {...props('numero_documento')}
            />
            <Campo
              etiqueta="Fecha de nacimiento"
              type="date"
              min="1900-01-01"
              max={hoy()}
              {...props('fecha_nacimiento')}
            />
            <Selector etiqueta="Sexo" {...props('sexo')}>
              <option value="">Sin indicar</option>
              {Object.entries(SEXOS).map(([codigo, texto]) => (
                <option key={codigo} value={codigo}>
                  {texto}
                </option>
              ))}
            </Selector>
          </div>
        </fieldset>

        <fieldset className={css.bloque}>
          <legend>Contacto</legend>
          <div className={css.columnas}>
            <Campo
              etiqueta="Teléfono"
              type="tel"
              required
              autoComplete="off"
              maxLength={25}
              ayuda="Con prefijo + si no es español."
              {...props('telefono')}
            />
            <Campo etiqueta="Correo" type="email" autoComplete="off" maxLength={150} {...props('email')} />
          </div>
          <Campo
            etiqueta="Dirección"
            autoComplete="off"
            maxLength={255}
            ayuda="Calle, número, código postal y localidad."
            {...props('direccion')}
          />
        </fieldset>

        <fieldset className={css.bloque}>
          <legend>Contacto de emergencia</legend>
          <div className={css.columnas}>
            <Campo
              etiqueta="Nombre del contacto"
              autoComplete="off"
              maxLength={100}
              ayuda="Puedes añadir la relación entre paréntesis: Ana Pérez (madre)."
              {...props('contacto_emergencia_nombre')}
            />
            <Campo
              etiqueta="Teléfono del contacto"
              type="tel"
              autoComplete="off"
              maxLength={25}
              {...props('contacto_emergencia_telefono')}
            />
          </div>
        </fieldset>

        <AreaDeTexto
          etiqueta="Observaciones administrativas"
          maxLength={1000}
          rows={3}
          ayuda="Por ejemplo, a qué hora prefiere que le llamen. No escribas aquí datos clínicos: irán en su expediente."
          {...props('observaciones')}
        />
      </div>
      {general && <Aviso tipo="error">{general}</Aviso>}
    </Dialogo>
  );
}
