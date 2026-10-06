import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Rutas } from '../../app/Rutas';
import type {
  Bloqueo,
  CitaResumen,
  Horarios,
  Odontologo,
  OdontologoConfigurado,
  TratamientoConfigurado,
  UsuarioParaOdontologo,
} from '../../shared/api/tipos';
import { pintar, respuestaJson, usuarioDePrueba } from '../../test/utilidades';
import { PERMISOS } from '../auth/permisos';

const COORDINACION = usuarioDePrueba(
  [
    PERMISOS.ODONTOLOGOS_GESTIONAR,
    PERMISOS.TRATAMIENTOS_GESTIONAR,
    PERMISOS.HORARIOS_GESTIONAR,
    PERMISOS.BLOQUEOS_GESTIONAR,
  ],
  [{ codigo: 'COORDINADOR', nombre: 'Coordinador' }],
);
const RECEPCION = usuarioDePrueba([PERMISOS.BLOQUEOS_GESTIONAR]);

const ANA: OdontologoConfigurado = {
  id: 3,
  nombre: 'Dra. Ana Villar',
  especialidad: 'Higiene y prevención',
  activo: true,
  usuario: { id: 11, username: 'ana', nombre: 'Ana Villar' },
  tratamientos: ['Limpieza dental', 'Valoración odontológica'],
};
const MARTA: OdontologoConfigurado = {
  id: 5,
  nombre: 'Dra. Marta Pérez',
  especialidad: 'Odontopediatría',
  activo: true,
  tratamientos: ['Valoración odontológica'],
};
const USUARIOS: UsuarioParaOdontologo[] = [
  {
    id: 11,
    username: 'ana',
    nombre: 'Ana Villar',
    roles: ['Odontólogo'],
    odontologo_id: 3,
    odontologo: 'Dra. Ana Villar',
  },
  { id: 12, username: 'marta', nombre: 'Marta Pérez', roles: ['Odontólogo'] },
];
// Los odontólogos activos, como los da la API pública que también usa la web
const EN_LA_WEB: Odontologo[] = [
  { id: 1, nombre: 'Dra. Rocío Fernández', especialidad: 'Odontología general' },
  { id: 3, nombre: 'Dra. Ana Villar', especialidad: 'Higiene y prevención' },
];
const LIMPIEZA: TratamientoConfigurado = {
  id: 2,
  nombre: 'Limpieza dental',
  descripcion_corta: 'Higiene profesional y revisión.',
  precio_desde: 60,
  duracion_aproximada: '45–60 min',
  duracion_minutos: 60,
  activo: true,
  odontologos: [{ id: 3, nombre: 'Dra. Ana Villar', activo: true }],
};
const LABORABLES = [1, 2, 3, 4, 5];
const HORARIOS: Horarios = {
  clinica: [
    ...LABORABLES.map((dia) => ({ dia_semana: dia, hora_apertura: '09:00', hora_cierre: '21:00' })),
    { dia_semana: 6, hora_apertura: '10:00', hora_cierre: '14:00' },
  ],
  odontologos: [
    {
      id: 3,
      nombre: 'Dra. Ana Villar',
      turnos: [
        ...LABORABLES.map((dia) => ({ dia_semana: dia, hora_inicio: '09:00', hora_fin: '13:00' })),
        { dia_semana: 6, hora_inicio: '09:00', hora_fin: '14:00' },
      ],
    },
  ],
};
const CITA: CitaResumen = {
  id: 40,
  fecha: '2026-10-13',
  hora_inicio: '10:00',
  hora_fin: '11:00',
  estado: 'CONFIRMADA',
  origen: 'LANDING',
  tratamiento_id: 2,
  tratamiento: 'Limpieza dental',
  odontologo_id: 3,
  odontologo: 'Dra. Ana Villar',
  paciente_id: 5,
  nombre: 'Lucía Romero',
  telefono: '600123456',
};
const ALMUERZO: Bloqueo = {
  id: 1,
  tipo: 'ALMUERZO',
  motivo: 'Almuerzo',
  dia_semana: 1,
  hora_inicio: '12:00',
  hora_fin: '13:00',
};

/** Sustituye a la API: responde según el método y la ruta (la primera que encaje) y apunta cada llamada. */
function api(respuestas: Record<string, () => Response>) {
  const llamadas: { metodo: string; url: string; cuerpo: unknown }[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn<typeof fetch>((entrada, init) => {
      const url = String(entrada);
      const metodo = init?.method ?? 'GET';
      llamadas.push({ metodo, url, cuerpo: init?.body ? JSON.parse(String(init.body)) : undefined });
      const respuesta = Object.entries(respuestas).find(([patron]) => `${metodo} ${url}`.startsWith(patron));
      return Promise.resolve(respuesta ? respuesta[1]() : respuestaJson({ message: 'No encontrado' }, 404));
    }),
  );
  return llamadas;
}

afterEach(() => vi.unstubAllGlobals());

describe('configuración de la agenda', () => {
  it('odontólogos: se vincula un usuario y no se ofrecen los que ya son de otro odontólogo', async () => {
    const llamadas = api({
      'GET /api/configuracion/odontologos/usuarios': () => respuestaJson(USUARIOS),
      'GET /api/configuracion/odontologos': () => respuestaJson([ANA, MARTA]),
      'PUT /api/configuracion/odontologos/5': () =>
        respuestaJson({ ...MARTA, usuario: { id: 12, username: 'marta', nombre: 'Marta Pérez' } }),
    });
    pintar(<Rutas />, { ruta: '/odontologos', auth: { usuario: COORDINACION } });

    const tabla = await screen.findByRole('table', { name: 'Odontólogos de la clínica' });
    expect(within(tabla).getByRole('row', { name: /Dra. Ana Villar/ })).toHaveTextContent('Ana Villarana');
    const fila = within(tabla).getByRole('row', { name: /Dra. Marta Pérez/ });
    expect(fila).toHaveTextContent('Sin usuario');
    expect(fila).toHaveTextContent('Valoración odontológica');

    await userEvent.click(within(fila).getByRole('button', { name: 'Editar a Dra. Marta Pérez' }));
    const dialogo = screen.getByRole('dialog', { name: 'Editar a Dra. Marta Pérez' });
    const usuario = within(dialogo).getByLabelText('Usuario del software');
    await within(dialogo).findByRole('option', { name: 'Marta Pérez (marta) · Odontólogo' });
    // Un usuario solo puede ser de un odontólogo (la API también lo comprueba)
    expect(
      within(dialogo).getByRole('option', {
        name: 'Ana Villar (ana) · Odontólogo · ya es de Dra. Ana Villar',
      }),
    ).toBeDisabled();
    await userEvent.selectOptions(usuario, '12');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }));

    expect(
      await screen.findByText(/Cuando Marta Pérez entre al software verá su agenda/),
    ).toBeInTheDocument();
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({
      nombre: 'Dra. Marta Pérez',
      especialidad: 'Odontopediatría',
      usuario_id: 12,
    });
  });

  it('tratamientos: la duración y quién lo hace los guarda la API; «Solo algunos» sin nadie no se envía', async () => {
    const llamadas = api({
      'GET /api/configuracion/tratamientos': () => respuestaJson([LIMPIEZA]),
      'GET /api/odontologos': () => respuestaJson(EN_LA_WEB),
      'PUT /api/configuracion/tratamientos/2': () =>
        respuestaJson({
          ...LIMPIEZA,
          duracion_minutos: 45,
          odontologos: [{ id: 1, nombre: 'Dra. Rocío Fernández', activo: true }],
        }),
    });
    pintar(<Rutas />, { ruta: '/tratamientos', auth: { usuario: COORDINACION } });

    const fila = await screen.findByRole('row', { name: /Limpieza dental/ });
    expect(fila).toHaveTextContent('60 min');
    expect(fila).toHaveTextContent('Orientativa: 45–60 min');
    expect(fila).toHaveTextContent('Desde 60 €');
    expect(fila).toHaveTextContent('Dra. Ana Villar');
    expect(fila).toHaveTextContent('Se ofrece');

    await userEvent.click(within(fila).getByRole('button', { name: 'Editar Limpieza dental' }));
    const dialogo = screen.getByRole('dialog', { name: 'Editar «Limpieza dental»' });
    const duracion = within(dialogo).getByLabelText(/Duración en la agenda/);
    await userEvent.clear(duracion);
    await userEvent.type(duracion, '45');
    expect(within(dialogo).getByRole('radio', { name: 'Solo algunos' })).toBeChecked();
    await userEvent.click(await within(dialogo).findByRole('checkbox', { name: /Dra. Ana Villar/ }));
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }));
    expect(
      within(dialogo).getByText('Marca al menos un odontólogo, o elige «Cualquier odontólogo».'),
    ).toBeInTheDocument();
    expect(llamadas.some((l) => l.metodo === 'PUT')).toBe(false);

    await userEvent.click(within(dialogo).getByRole('checkbox', { name: /Dra. Rocío Fernández/ }));
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }));
    expect(
      await screen.findByText(/Las citas nuevas ocuparán 45 minutos; las que ya estaban dadas no cambian/),
    ).toBeInTheDocument();
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({
      nombre: 'Limpieza dental',
      descripcion_corta: 'Higiene profesional y revisión.',
      duracion_minutos: 45,
      duracion_aproximada: '45–60 min',
      precio_desde: 60,
      odontologo_ids: [1],
    });
  });

  it('horarios: al cambiar los turnos de una odontóloga se avisa de las citas que quedan fuera', async () => {
    const llamadas = api({
      'GET /api/configuracion/horarios': () => respuestaJson(HORARIOS),
      'PUT /api/configuracion/horarios/odontologos/3': () =>
        respuestaJson({ horarios: HORARIOS, citas_afectadas: { total: 1, citas: [CITA] } }),
    });
    pintar(<Rutas />, { ruta: '/horarios', auth: { usuario: COORDINACION } });

    const clinica = await screen.findByRole('region', { name: 'Horario de la clínica' });
    expect(within(clinica).getAllByText('09:00 a 21:00')).toHaveLength(5);
    expect(within(clinica).getByText('Cerrado')).toBeInTheDocument();
    const ana = screen.getByRole('region', { name: 'Dra. Ana Villar' });
    expect(within(ana).getAllByText('09:00 a 13:00')).toHaveLength(5);
    expect(within(ana).getByText('No trabaja')).toBeInTheDocument();
    // El sábado empieza antes de que abra la clínica: se avisa (los huecos los calcula la API con los dos)
    expect(within(ana).getByText('Solo cuenta de 10:00 a 14:00, cuando abre la clínica')).toBeInTheDocument();

    await userEvent.click(within(ana).getByRole('button', { name: 'Cambiar turnos de Dra. Ana Villar' }));
    const dialogo = screen.getByRole('dialog', { name: 'Turnos de Dra. Ana Villar' });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Quitar el tramo del martes' }));
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Añadir otro tramo el lunes' }));
    // Se propone desde donde acaba el primer tramo hasta que cierra la clínica
    expect(within(dialogo).getByLabelText('Empieza el lunes (2.º tramo)')).toHaveValue('13:00');
    expect(within(dialogo).getByLabelText('Termina el lunes (2.º tramo)')).toHaveValue('21:00');
    fireEvent.change(within(dialogo).getByLabelText('Termina el lunes (2.º tramo)'), {
      target: { value: '17:00' },
    });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar turnos' }));

    expect(await screen.findByText(/Se han guardado los turnos de Dra. Ana Villar/)).toBeInTheDocument();
    const revisar = screen.getByRole('region', { name: '1 cita por revisar' });
    expect(revisar).toHaveTextContent('quedan fuera de sus turnos nuevos');
    expect(within(revisar).getByRole('link', { name: /Lucía Romero/ })).toHaveAttribute(
      'href',
      '/agenda?fecha=2026-10-13&cita=40',
    );
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({
      turnos: [
        { dia_semana: 1, hora_inicio: '09:00', hora_fin: '13:00' },
        { dia_semana: 1, hora_inicio: '13:00', hora_fin: '17:00' },
        { dia_semana: 3, hora_inicio: '09:00', hora_fin: '13:00' },
        { dia_semana: 4, hora_inicio: '09:00', hora_fin: '13:00' },
        { dia_semana: 5, hora_inicio: '09:00', hora_fin: '13:00' },
        { dia_semana: 6, hora_inicio: '09:00', hora_fin: '14:00' },
      ],
    });
  });

  it('bloqueos: un festivo para toda la clínica, y quitar uno pide confirmación', async () => {
    const llamadas = api({
      'GET /api/configuracion/bloqueos': () => respuestaJson([ALMUERZO]),
      'GET /api/odontologos': () => respuestaJson(EN_LA_WEB),
      'POST /api/configuracion/bloqueos': () =>
        respuestaJson(
          {
            bloqueo: { id: 9, tipo: 'FERIADO', fecha_inicio: '2026-10-13', fecha_fin: '2026-10-13' },
            citas_afectadas: { total: 2 },
          },
          201,
        ),
      'DELETE /api/configuracion/bloqueos/1': () => new Response(null, { status: 204 }),
    });
    pintar(<Rutas />, { ruta: '/bloqueos', auth: { usuario: RECEPCION } });

    const fila = await screen.findByRole('row', { name: /Almuerzo/ });
    expect(fila).toHaveTextContent('Toda la clínica');
    expect(fila).toHaveTextContent('Cada lunes');
    expect(fila).toHaveTextContent('12:00 a 13:00');

    await userEvent.click(screen.getByRole('button', { name: 'Nuevo bloqueo' }));
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo bloqueo' });
    await userEvent.selectOptions(within(dialogo).getByLabelText('Tipo'), 'FERIADO');
    fireEvent.change(within(dialogo).getByLabelText(/Primer día/), { target: { value: '2026-10-13' } });
    // «Solo unas horas» sin horas bloquearía el día entero: se piden antes de enviar
    await userEvent.click(within(dialogo).getByRole('radio', { name: 'Solo unas horas' }));
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Bloquear' }));
    expect(within(dialogo).getByText('Indica la hora de inicio, o marca «Todo el día».')).toBeInTheDocument();
    expect(llamadas.some((l) => l.metodo === 'POST')).toBe(false);

    await userEvent.click(within(dialogo).getByRole('radio', { name: 'Todo el día' }));
    await userEvent.type(within(dialogo).getByLabelText('Motivo'), 'Fiesta local');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Bloquear' }));
    expect(await screen.findByText(/Bloqueo guardado/)).toBeInTheDocument();
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({
      tipo: 'FERIADO',
      motivo: 'Fiesta local',
      fecha_inicio: '2026-10-13',
    });
    // Sin permiso para ver todas las agendas, la API solo dice cuántas citas quedan dentro
    const revisar = screen.getByRole('region', { name: '2 citas por revisar' });
    expect(revisar).toHaveTextContent('caen dentro de este bloqueo');
    expect(revisar).toHaveTextContent('avisa a recepción para que las revise');

    await userEvent.click(screen.getByRole('button', { name: 'Quitar Almuerzo, cada lunes' }));
    const confirmar = screen.getByRole('dialog', { name: '¿Quitar este bloqueo?' });
    await userEvent.click(within(confirmar).getByRole('button', { name: 'Quitar bloqueo' }));
    expect(await screen.findByText(/Bloqueo quitado/)).toBeInTheDocument();
    expect(llamadas.some((l) => l.metodo === 'DELETE' && l.url === '/api/configuracion/bloqueos/1')).toBe(
      true,
    );
  });
});
