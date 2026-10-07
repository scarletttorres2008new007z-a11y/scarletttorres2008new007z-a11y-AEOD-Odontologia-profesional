import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Rutas } from '../../app/Rutas';
import type { Agenda, Cita, CitaResumen, Paciente } from '../../shared/api/tipos';
import { pintar, respuestaJson, usuarioDePrueba } from '../../test/utilidades';
import { PERMISOS } from '../auth/permisos';

const RECEPCION = usuarioDePrueba([
  PERMISOS.PACIENTES_VER,
  PERMISOS.PACIENTES_CREAR,
  PERMISOS.CITAS_VER,
  PERMISOS.CITAS_VER_TODAS,
  PERMISOS.CITAS_CREAR,
  PERMISOS.CITAS_CAMBIAR_ESTADO,
  PERMISOS.CITAS_CANCELAR,
  PERMISOS.CITAS_REPROGRAMAR,
  PERMISOS.CITAS_EDITAR,
]);
const ODONTOLOGA = usuarioDePrueba(
  [PERMISOS.PACIENTES_VER, PERMISOS.CITAS_VER],
  [{ codigo: 'ODONTOLOGO', nombre: 'Odontólogo' }],
);

const JOSE: Paciente = {
  id: 5,
  codigo: 'K7M3QX',
  nombres: 'José Luis',
  apellidos: 'García Pérez',
  telefono: '+34600123456',
  activo: true,
  creado_en: '2026-10-01T08:00:00',
  actualizado_en: '2026-10-01T08:00:00',
};

const DE_LA_WEB: CitaResumen = {
  id: 41,
  fecha: '2026-10-12',
  hora_inicio: '10:00',
  hora_fin: '11:00',
  estado: 'PENDIENTE',
  origen: 'LANDING',
  tratamiento_id: 2,
  tratamiento: 'Limpieza dental',
  odontologo_id: 3,
  odontologo: 'Dra. Ana Villar',
  nombre: 'Lucía Romero',
  telefono: '600 111 222',
};

const DE_LA_CLINICA: CitaResumen = {
  ...DE_LA_WEB,
  id: 42,
  hora_inicio: '09:00',
  hora_fin: '09:30',
  estado: 'CONFIRMADA',
  origen: 'SOFTWARE',
  tratamiento_id: 1,
  tratamiento: 'Valoración',
  odontologo_id: 1,
  odontologo: 'Dra. Rocío Fernández',
  paciente_id: 5,
  nombre: 'José Luis García Pérez',
  telefono: '+34600123456',
};

function agenda(cambios: Partial<Agenda> = {}): Agenda {
  return {
    desde: '2026-10-12',
    hasta: '2026-10-12',
    dias: [{ fecha: '2026-10-12', apertura: '09:00', cierre: '21:00' }],
    odontologos: [
      { id: 1, nombre: 'Dra. Rocío Fernández' },
      { id: 3, nombre: 'Dra. Ana Villar' },
    ],
    turnos: [
      { odontologo_id: 1, fecha: '2026-10-12', hora_inicio: '09:00', hora_fin: '17:00' },
      { odontologo_id: 3, fecha: '2026-10-12', hora_inicio: '09:00', hora_fin: '13:00' },
    ],
    bloqueos: [{ fecha: '2026-10-12', hora_inicio: '12:00', hora_fin: '13:00', tipo: 'ALMUERZO' }],
    citas: [DE_LA_CLINICA, DE_LA_WEB],
    solo_su_agenda: false,
    ...cambios,
  };
}

function detalle(cambios: Partial<Cita> = {}): Cita {
  return {
    id: 41,
    fecha: '2026-10-12',
    hora_inicio: '10:00',
    hora_fin: '11:00',
    duracion_minutos: 60,
    estado: 'PENDIENTE',
    origen: 'LANDING',
    tratamiento_id: 2,
    tratamiento: 'Limpieza dental',
    odontologo_id: 3,
    odontologo: 'Dra. Ana Villar',
    contacto: {
      nombre: 'Lucía Romero',
      telefono: '600 111 222',
      email: 'lucia@example.com',
      mensaje: 'Me sangran las encías',
    },
    creada_en: '2026-10-06T17:05:00',
    actualizada_en: '2026-10-06T17:05:00',
    estados_siguientes: ['CONFIRMADA', 'EN_ATENCION', 'NO_ASISTIO'],
    modificable: true,
    ...cambios,
  };
}

const ODONTOLOGOS = [
  { id: 1, nombre: 'Dra. Rocío Fernández' },
  { id: 2, nombre: 'Dr. Marcos Ortega' },
  { id: 3, nombre: 'Dra. Ana Villar' },
];
const TRATAMIENTOS = [
  { id: 1, nombre: 'Valoración', duracion_minutos: 30, odontologo_ids: [] },
  { id: 2, nombre: 'Limpieza dental', duracion_minutos: 60, odontologo_ids: [1, 3] },
];

type Respuestas = Record<string, () => Response>;

/** Sustituye a la API: responde según el método y el principio de la ruta, y apunta cada llamada. */
function api(respuestas: Respuestas) {
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

beforeEach(() => {
  // Lunes 12 de octubre de 2026, 08:00 en Madrid
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date('2026-10-12T06:00:00Z'));
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('agenda', () => {
  it('pinta cada cita de la API en la columna de su odontólogo y la abre con sus datos', async () => {
    let cita41 = detalle();
    const llamadas = api({
      'GET /api/agenda?': () => respuestaJson(agenda()),
      'GET /api/odontologos': () => respuestaJson(ODONTOLOGOS),
      'GET /api/citas/41': () => respuestaJson(cita41),
      'PUT /api/citas/41/estado': () => {
        cita41 = detalle({ estado: 'CONFIRMADA', estados_siguientes: ['EN_ATENCION', 'NO_ASISTIO'] });
        return respuestaJson(cita41);
      },
    });
    pintar(<Rutas />, { ruta: '/agenda', auth: { usuario: RECEPCION } });

    expect(
      await screen.findByRole('heading', { name: 'Lunes 12 de octubre de 2026', level: 2 }),
    ).toBeInTheDocument();
    expect(llamadas.find((l) => l.url.startsWith('/api/agenda?'))?.url).toBe(
      '/api/agenda?desde=2026-10-12&dias=1',
    );

    const ana = await screen.findByRole('region', { name: 'Agenda de Dra. Ana Villar' });
    const cita = within(ana).getByRole('link', { name: /Lucía Romero/ });
    expect(cita).toHaveTextContent('10:00');
    expect(cita).toHaveTextContent('Limpieza dental · Por confirmar · Sin ficha');
    expect(cita).toHaveAttribute('href', '/agenda?cita=41');
    expect(within(ana).getByText('Almuerzo')).toBeInTheDocument();
    const rocio = screen.getByRole('region', { name: 'Agenda de Dra. Rocío Fernández' });
    expect(within(rocio).getByRole('link', { name: /José Luis García Pérez/ })).toHaveTextContent(
      'Valoración · Confirmada',
    );
    expect(within(rocio).getByText('Almuerzo')).toBeInTheDocument();

    await userEvent.click(cita);
    const dialogo = await screen.findByRole('dialog', { name: 'Cita de Lucía Romero' });
    expect(await within(dialogo).findByText('Lunes 12 de octubre de 2026')).toBeInTheDocument();
    expect(dialogo).toHaveTextContent('10:00 a 11:00 (60 min)');
    expect(dialogo).toHaveTextContent('Sin ficha todavía');
    expect(dialogo).toHaveTextContent('Me sangran las encías');
    expect(dialogo).toHaveTextContent('Reservada en la web el 06/10/2026 17:05.');
    expect(within(dialogo).getByRole('button', { name: 'Vincular a un paciente' })).toBeInTheDocument();
    expect(within(dialogo).getByRole('button', { name: 'Mover a otro día u hora' })).toBeInTheDocument();

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar' }));
    expect(await within(dialogo).findByText('Cita confirmada.')).toBeInTheDocument();
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ estado: 'CONFIRMADA' });
    expect(within(dialogo).getByRole('button', { name: 'Pasa a consulta' })).toBeInTheDocument();
  });

  it('da cita en uno de los huecos libres que calcula la API', async () => {
    const llamadas = api({
      'GET /api/agenda?': () => respuestaJson(agenda({ citas: [] })),
      'GET /api/odontologos': () => respuestaJson(ODONTOLOGOS),
      'GET /api/tratamientos': () => respuestaJson(TRATAMIENTOS),
      'GET /api/pacientes?': () =>
        respuestaJson({ contenido: [JOSE], pagina: 0, tamano: 6, total_elementos: 1, total_paginas: 1 }),
      'GET /api/agenda/disponibilidad?': () =>
        respuestaJson([
          { odontologo_id: 1, odontologo: 'Dra. Rocío Fernández', hora_inicio: '10:00', hora_fin: '11:00' },
          { odontologo_id: 3, odontologo: 'Dra. Ana Villar', hora_inicio: '09:00', hora_fin: '10:00' },
          { odontologo_id: 3, odontologo: 'Dra. Ana Villar', hora_inicio: '11:00', hora_fin: '12:00' },
        ]),
      'POST /api/pacientes/5/citas': () =>
        respuestaJson(
          detalle({
            id: 50,
            fecha: '2026-10-13',
            hora_inicio: '11:00',
            hora_fin: '12:00',
            estado: 'CONFIRMADA',
            origen: 'SOFTWARE',
          }),
          201,
        ),
    });
    pintar(<Rutas />, { ruta: '/agenda?fecha=2026-10-13', auth: { usuario: RECEPCION } });

    await userEvent.click(await screen.findByRole('button', { name: 'Nueva cita' }));
    const dialogo = screen.getByRole('dialog', { name: 'Nueva cita' });
    await userEvent.type(within(dialogo).getByLabelText('Buscar paciente'), 'garcia');
    await userEvent.click(await within(dialogo).findByRole('button', { name: /García Pérez, José Luis/ }));
    expect(within(dialogo).getByText('José Luis García Pérez')).toBeInTheDocument();

    expect(
      within(dialogo).getByText('Elige el tratamiento y el día para ver los huecos libres.'),
    ).toBeInTheDocument();
    const tratamiento = within(dialogo).getByLabelText('Tratamiento');
    expect(within(tratamiento).getByRole('option', { name: 'Limpieza dental · 60 min' })).toBeInTheDocument();
    await userEvent.selectOptions(tratamiento, '2');
    // Solo se ofrecen los odontólogos que la API indica para ese tratamiento
    const odontologo = within(dialogo).getByLabelText('Odontólogo');
    expect(
      within(odontologo)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Cualquiera que lo haga', 'Dra. Rocío Fernández', 'Dra. Ana Villar']);

    const deAna = await within(dialogo).findByRole('group', { name: 'Dra. Ana Villar' });
    await userEvent.click(within(deAna).getByRole('radio', { name: /^11:00/ }));
    await userEvent.type(within(dialogo).getByLabelText('Notas internas'), 'Primera visita');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Dar cita' }));

    expect(
      await screen.findByText(
        /Cita dada a José Luis García Pérez el martes 13 de octubre a las 11:00 con Dra. Ana Villar/,
      ),
    ).toBeInTheDocument();
    expect(llamadas.find((l) => l.url.startsWith('/api/agenda/disponibilidad'))?.url).toBe(
      '/api/agenda/disponibilidad?tratamiento_id=2&fecha=2026-10-13',
    );
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({
      tratamiento_id: 2,
      odontologo_id: 3,
      fecha: '2026-10-13',
      hora_inicio: '11:00',
      notas_internas: 'Primera visita',
    });
  });

  it('si otra persona coge el hueco antes, lo dice y vuelve a pedir los libres', async () => {
    const llamadas = api({
      'GET /api/agenda?': () => respuestaJson(agenda({ citas: [] })),
      'GET /api/odontologos': () => respuestaJson(ODONTOLOGOS),
      'GET /api/tratamientos': () => respuestaJson(TRATAMIENTOS),
      'GET /api/pacientes/5': () => respuestaJson(JOSE),
      'GET /api/citas?': () =>
        respuestaJson({ contenido: [], pagina: 0, tamano: 10, total_elementos: 0, total_paginas: 0 }),
      'GET /api/agenda/disponibilidad?': () =>
        respuestaJson([
          { odontologo_id: 3, odontologo: 'Dra. Ana Villar', hora_inicio: '09:00', hora_fin: '10:00' },
        ]),
      'POST /api/pacientes/5/citas': () =>
        respuestaJson(
          { status: 409, message: 'Ese horario acaba de ocuparse. Elige otro de los disponibles.' },
          409,
        ),
    });
    pintar(<Rutas />, { ruta: '/pacientes/5', auth: { usuario: RECEPCION } });

    // Desde la ficha, el paciente ya viene elegido
    const citas = await screen.findByRole('region', { name: 'Citas' });
    expect(await within(citas).findByText('No tiene citas a partir de hoy.')).toBeInTheDocument();
    await userEvent.click(within(citas).getByRole('button', { name: 'Dar cita' }));
    const dialogo = screen.getByRole('dialog', { name: 'Dar cita a José Luis García Pérez' });
    await userEvent.selectOptions(within(dialogo).getByLabelText('Tratamiento'), '2');
    await userEvent.click(await within(dialogo).findByRole('radio', { name: /^09:00/ }));
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Dar cita' }));

    expect(
      await within(dialogo).findByText('Ese horario acaba de ocuparse. Elige otro de los disponibles.'),
    ).toBeInTheDocument();
    await vi.waitFor(() =>
      expect(llamadas.filter((l) => l.url.startsWith('/api/agenda/disponibilidad'))).toHaveLength(2),
    );
    expect(within(dialogo).getByRole('radio', { name: /^09:00/ })).not.toBeChecked();
  });

  it('cancelar pide confirmación, envía el motivo y deja la cita como cancelada', async () => {
    let cita41 = detalle({ estado: 'CONFIRMADA' });
    const llamadas = api({
      'GET /api/agenda?': () => respuestaJson(agenda()),
      'GET /api/odontologos': () => respuestaJson(ODONTOLOGOS),
      'GET /api/citas/41': () => respuestaJson(cita41),
      'POST /api/citas/41/cancelacion': () => {
        cita41 = detalle({
          estado: 'CANCELADA',
          estados_siguientes: [],
          modificable: false,
          cancelada_en: '2026-10-12T08:01:00',
          cancelada_por: 'SOFTWARE',
          motivo_cancelacion: 'Avisó por teléfono',
        });
        return respuestaJson(cita41);
      },
    });
    pintar(<Rutas />, { ruta: '/agenda?cita=41', auth: { usuario: RECEPCION } });

    const dialogo = await screen.findByRole('dialog', { name: 'Cita de Lucía Romero' });
    await userEvent.click(await within(dialogo).findByRole('button', { name: 'Cancelar la cita' }));
    const confirmar = screen.getByRole('dialog', { name: '¿Cancelar esta cita?' });
    expect(confirmar).toHaveTextContent('No se borra');
    await userEvent.type(within(confirmar).getByLabelText('Motivo'), 'Avisó por teléfono');
    await userEvent.click(within(confirmar).getByRole('button', { name: 'Cancelar la cita' }));

    const otraVez = await screen.findByRole('dialog', { name: 'Cita de Lucía Romero' });
    expect(within(otraVez).getByText(/Cita cancelada\. Su hueco vuelve a estar libre/)).toBeInTheDocument();
    expect(otraVez).toHaveTextContent(
      'Cancelada desde la clínica el 12/10/2026 08:01. Motivo: Avisó por teléfono',
    );
    expect(within(otraVez).queryByRole('button', { name: 'Cancelar la cita' })).not.toBeInTheDocument();
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({ motivo: 'Avisó por teléfono' });
  });

  it('vincula una reserva de la web a la ficha del paciente buscándolo por su teléfono', async () => {
    let cita41 = detalle();
    const llamadas = api({
      'GET /api/agenda?': () => respuestaJson(agenda()),
      'GET /api/odontologos': () => respuestaJson(ODONTOLOGOS),
      'GET /api/citas/41': () => respuestaJson(cita41),
      'GET /api/pacientes?': () =>
        respuestaJson({ contenido: [JOSE], pagina: 0, tamano: 6, total_elementos: 1, total_paginas: 1 }),
      'PUT /api/citas/41/paciente': () => {
        cita41 = detalle({
          paciente: { id: 5, codigo: 'K7M3QX', nombre: 'José Luis García Pérez', telefono: '+34600123456' },
        });
        return respuestaJson(cita41);
      },
    });
    pintar(<Rutas />, { ruta: '/agenda?cita=41', auth: { usuario: RECEPCION } });

    const dialogo = await screen.findByRole('dialog', { name: 'Cita de Lucía Romero' });
    await userEvent.click(await within(dialogo).findByRole('button', { name: 'Vincular a un paciente' }));
    const vincular = screen.getByRole('dialog', { name: 'Vincular la cita a un paciente' });
    expect(within(vincular).getByLabelText('Buscar paciente')).toHaveValue('600111222');
    await userEvent.click(await within(vincular).findByRole('button', { name: /García Pérez, José Luis/ }));
    await userEvent.click(within(vincular).getByRole('button', { name: 'Vincular' }));

    const otraVez = await screen.findByRole('dialog', { name: 'Cita de José Luis García Pérez' });
    expect(
      within(otraVez).getByText('La cita ha quedado en la ficha de José Luis García Pérez.'),
    ).toBeInTheDocument();
    expect(within(otraVez).getByRole('link', { name: 'José Luis García Pérez' })).toHaveAttribute(
      'href',
      '/pacientes/5',
    );
    expect(llamadas.find((l) => l.url.startsWith('/api/pacientes?'))?.url).toBe(
      '/api/pacientes?texto=600111222&activo=true&pagina=0&tamano=6',
    );
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ paciente_id: 5 });
  });

  it('quien solo ve su agenda no puede elegir otros odontólogos ni cambiar citas', async () => {
    api({
      'GET /api/agenda?': () =>
        respuestaJson(
          agenda({
            odontologos: [{ id: 3, nombre: 'Dra. Ana Villar' }],
            citas: [DE_LA_WEB],
            solo_su_agenda: true,
          }),
        ),
      'GET /api/odontologos': () => respuestaJson(ODONTOLOGOS),
      'GET /api/citas/41': () => respuestaJson(detalle({ estado: 'CONFIRMADA' })),
    });
    pintar(<Rutas />, { ruta: '/agenda', auth: { usuario: ODONTOLOGA } });

    expect(await screen.findByText('Tus citas, las de la web y las que da la clínica.')).toBeInTheDocument();
    expect(screen.queryByLabelText('Odontólogo')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Nueva cita' })).not.toBeInTheDocument();

    await userEvent.click(await screen.findByRole('link', { name: /Lucía Romero/ }));
    const dialogo = await screen.findByRole('dialog', { name: 'Cita de Lucía Romero' });
    await within(dialogo).findByText('Limpieza dental');
    expect(
      within(dialogo)
        .getAllByRole('button')
        .map((b) => b.textContent),
    ).toEqual(['×', 'Cerrar']);
  });

  it('la semana lista las citas de cada día y enlaza a su vista de día', async () => {
    const llamadas = api({
      'GET /api/agenda?': () =>
        respuestaJson(
          agenda({
            desde: '2026-10-12',
            hasta: '2026-10-18',
            dias: [
              { fecha: '2026-10-12', apertura: '09:00', cierre: '21:00' },
              { fecha: '2026-10-13', apertura: '09:00', cierre: '21:00' },
              { fecha: '2026-10-18' },
            ],
            citas: [DE_LA_CLINICA, { ...DE_LA_WEB, fecha: '2026-10-13' }],
          }),
        ),
      'GET /api/odontologos': () => respuestaJson(ODONTOLOGOS),
    });
    pintar(<Rutas />, { ruta: '/agenda?fecha=2026-10-14&vista=semana', auth: { usuario: RECEPCION } });

    expect(
      await screen.findByRole('heading', { name: 'Semana del 12 al 18 de octubre de 2026', level: 2 }),
    ).toBeInTheDocument();
    expect(llamadas.find((l) => l.url.startsWith('/api/agenda?'))?.url).toBe(
      '/api/agenda?desde=2026-10-12&dias=7',
    );
    const martes = await screen.findByRole('link', { name: 'Martes 13 de octubre' });
    expect(martes).toHaveAttribute('href', '/agenda?fecha=2026-10-13');
    expect(screen.getByRole('link', { name: /Lucía Romero/ })).toHaveTextContent(
      'Limpieza dental · Dra. Ana Villar',
    );
    expect(screen.getByText('Cerrado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Día', pressed: false })).toBeInTheDocument();
  });
});

describe('inicio', () => {
  it('enseña las citas de hoy y las reservas por confirmar', async () => {
    api({
      'GET /api/agenda?': () => respuestaJson(agenda()),
      'GET /api/citas?': () =>
        respuestaJson({
          contenido: [{ ...DE_LA_WEB, fecha: '2026-10-14' }],
          pagina: 0,
          tamano: 5,
          total_elementos: 1,
          total_paginas: 1,
        }),
    });
    pintar(<Rutas />, { auth: { usuario: RECEPCION } });

    const hoy = await screen.findByRole('region', { name: 'Citas de hoy' });
    const filas = await within(hoy).findAllByRole('link', { name: /Valoración|Limpieza/ });
    expect(filas.map((fila) => fila.textContent)).toEqual([
      '09:00José Luis García PérezValoración · Dra. Rocío FernándezConfirmada',
      '10:00Lucía RomeroLimpieza dental · Dra. Ana VillarPor confirmar',
    ]);
    const pendientes = await screen.findByRole('region', { name: 'Por confirmar (1)' });
    const fila = await within(pendientes).findByRole('link', { name: /Lucía Romero/ });
    expect(fila).toHaveAccessibleName(/^Miércoles 14 de octubre, 10:00/);
    expect(fila).toHaveTextContent('Limpieza dental · Sin ficha');
    expect(within(fila).getByText('mié 14 oct')).toHaveAttribute('aria-hidden', 'true');
    expect(fila).toHaveAttribute('href', '/agenda?fecha=2026-10-14&cita=41');
  });
});
