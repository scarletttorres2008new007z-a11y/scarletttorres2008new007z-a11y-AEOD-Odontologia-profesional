import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Rutas } from '../../app/Rutas';
import type { Paciente } from '../../shared/api/tipos';
import { pintar, respuestaJson, usuarioDePrueba } from '../../test/utilidades';
import { PERMISOS } from '../auth/permisos';

const SOLO_VER = usuarioDePrueba([PERMISOS.PACIENTES_VER], [{ codigo: 'ODONTOLOGO', nombre: 'Odontólogo' }]);
const RECEPCION = usuarioDePrueba([
  PERMISOS.PACIENTES_VER,
  PERMISOS.PACIENTES_CREAR,
  PERMISOS.PACIENTES_EDITAR,
]);

const JOSE: Paciente = {
  id: 5,
  codigo: 'K7M3QX',
  nombres: 'José Luis',
  apellidos: 'García Pérez',
  tipo_documento: 'DNI',
  numero_documento: '12345678Z',
  fecha_nacimiento: '1990-03-12',
  edad: 36,
  telefono: '+34600123456',
  email: 'jl.garcia@example.com',
  activo: true,
  creado_en: '2026-10-12T08:00:00',
  actualizado_en: '2026-10-12T08:00:00',
};

function pagina(contenido: Paciente[]) {
  return { contenido, pagina: 0, tamano: 20, total_elementos: contenido.length, total_paginas: 1 };
}

/** Sustituye a la API: responde según el método y la ruta, y apunta cada llamada. */
function api(respuestas: Record<string, () => Response>) {
  const llamadas: { metodo: string; url: string; cuerpo: unknown }[] = [];
  const fetchFalso = vi.fn<typeof fetch>((entrada, init) => {
    const url = String(entrada);
    const metodo = init?.method ?? 'GET';
    llamadas.push({ metodo, url, cuerpo: init?.body ? JSON.parse(String(init.body)) : undefined });
    const respuesta = Object.entries(respuestas).find(([patron]) => `${metodo} ${url}`.startsWith(patron));
    return Promise.resolve(respuesta ? respuesta[1]() : respuestaJson({ message: 'No encontrado' }, 404));
  });
  vi.stubGlobal('fetch', fetchFalso);
  return llamadas;
}

afterEach(() => vi.unstubAllGlobals());

describe('pacientes', () => {
  it('quien solo puede consultar ve la lista sin botones para cambiar nada', async () => {
    const llamadas = api({ 'GET /api/pacientes?': () => respuestaJson(pagina([JOSE])) });
    pintar(<Rutas />, { ruta: '/pacientes', auth: { usuario: SOLO_VER } });

    const tabla = await screen.findByRole('table', { name: /Pacientes/ });
    const fila = within(tabla).getByRole('row', { name: /García Pérez, José Luis/ });
    expect(within(fila).getByRole('link', { name: 'García Pérez, José Luis' })).toHaveAttribute(
      'href',
      '/pacientes/5',
    );
    expect(fila).toHaveTextContent('Código K7M3QX');
    expect(fila).toHaveTextContent('DNI 12345678Z');
    expect(fila).toHaveTextContent('+34 600 123 456');
    expect(fila).toHaveTextContent('36 años');
    expect(fila).toHaveTextContent('Activo');
    expect(screen.queryByRole('button', { name: 'Nuevo paciente' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Editar/ })).not.toBeInTheDocument();
    expect(llamadas.at(0)?.url).toBe('/api/pacientes?pagina=0&tamano=20');
  });

  it('el alta enseña los errores de la API junto a cada campo y lleva el foco al primero', async () => {
    const llamadas = api({
      'GET /api/pacientes?': () => respuestaJson(pagina([])),
      'POST /api/pacientes': () =>
        respuestaJson(
          {
            status: 400,
            message: 'Revisa los datos enviados.',
            errores: {
              apellidos: 'Los apellidos son obligatorios.',
              numero_documento: 'La letra no corresponde a ese número de DNI. Revísalo.',
            },
          },
          400,
        ),
    });
    pintar(<Rutas />, { ruta: '/pacientes', auth: { usuario: RECEPCION } });

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo paciente' }));
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo paciente' });
    await userEvent.type(within(dialogo).getByLabelText(/^Nombre( \*)?$/), '  Ana ');
    await userEvent.selectOptions(within(dialogo).getByLabelText('Documento'), 'DNI');
    await userEvent.type(within(dialogo).getByLabelText('Número de documento'), '12345678A');
    await userEvent.type(within(dialogo).getByLabelText(/^Teléfono( \*)?$/), '600 111 222');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Dar de alta' }));

    expect(await within(dialogo).findByText('Los apellidos son obligatorios.')).toBeInTheDocument();
    expect(
      within(dialogo).getByText('La letra no corresponde a ese número de DNI. Revísalo.'),
    ).toBeInTheDocument();
    await vi.waitFor(() => expect(within(dialogo).getByLabelText(/^Apellidos/)).toHaveFocus());
    // Se envía lo escrito, sin espacios de más y sin los campos vacíos
    expect(llamadas.at(-1)?.cuerpo).toEqual({
      nombres: 'Ana',
      apellidos: '',
      tipo_documento: 'DNI',
      numero_documento: '12345678A',
      telefono: '600 111 222',
    });
  });

  it('tras el alta confirma el código del paciente y enlaza su ficha', async () => {
    api({
      'GET /api/pacientes?': () => respuestaJson(pagina([])),
      'POST /api/pacientes': () => respuestaJson(JOSE, 201),
    });
    pintar(<Rutas />, { ruta: '/pacientes', auth: { usuario: RECEPCION } });

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo paciente' }));
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo paciente' });
    await userEvent.type(within(dialogo).getByLabelText(/^Nombre( \*)?$/), 'José Luis');
    await userEvent.type(within(dialogo).getByLabelText(/^Apellidos/), 'García Pérez');
    await userEvent.type(within(dialogo).getByLabelText(/^Teléfono( \*)?$/), '+34 600 123 456');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Dar de alta' }));

    const aviso = await screen.findByText(/Se ha dado de alta a José Luis García Pérez/);
    expect(aviso).toHaveTextContent('Su código de paciente es K7M3QX.');
    expect(screen.getByRole('link', { name: 'Ver su ficha' })).toHaveAttribute('href', '/pacientes/5');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('la ficha muestra sus datos y deja dar de baja a quien puede editar', async () => {
    const llamadas = api({
      'GET /api/pacientes/5': () => respuestaJson(JOSE),
      'PUT /api/pacientes/5/estado': () => respuestaJson({ ...JOSE, activo: false }),
    });
    pintar(<Rutas />, { ruta: '/pacientes/5', auth: { usuario: RECEPCION } });

    expect(
      await screen.findByRole('heading', { name: 'José Luis García Pérez', level: 1 }),
    ).toBeInTheDocument();
    const personales = screen.getByRole('region', { name: 'Datos personales' });
    expect(personales).toHaveTextContent('DNI 12345678Z');
    expect(personales).toHaveTextContent('12/03/1990 (36 años)');
    expect(personales).toHaveTextContent('SexoSin indicar');
    expect(screen.getByRole('link', { name: '+34 600 123 456' })).toHaveAttribute('href', 'tel:+34600123456');
    expect(screen.getByText('Sin observaciones.')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Dar de baja' }));
    const dialogo = screen.getByRole('dialog', { name: '¿Dar de baja a José Luis García Pérez?' });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Dar de baja' }));

    expect(await screen.findByText('Se ha dado de baja a José Luis García Pérez.')).toBeInTheDocument();
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ activo: false });
  });

  it('en la ficha, quien solo consulta no ve botones para cambiarla', async () => {
    api({ 'GET /api/pacientes/5': () => respuestaJson(JOSE) });
    pintar(<Rutas />, { ruta: '/pacientes/5', auth: { usuario: SOLO_VER } });

    expect(
      await screen.findByRole('heading', { name: 'José Luis García Pérez', level: 1 }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Editar datos' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Dar de baja' })).not.toBeInTheDocument();
  });

  it('un paciente que no existe lo dice', async () => {
    api({
      'GET /api/pacientes/99': () => respuestaJson({ status: 404, message: 'El paciente no existe.' }, 404),
    });
    pintar(<Rutas />, { ruta: '/pacientes/99', auth: { usuario: SOLO_VER } });
    expect(await screen.findByRole('heading', { name: 'Este paciente no existe' })).toBeInTheDocument();
  });
});
