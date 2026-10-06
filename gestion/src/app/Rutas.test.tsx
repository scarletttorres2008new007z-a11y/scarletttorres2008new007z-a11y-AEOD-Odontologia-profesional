import { screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PERMISOS } from '../features/auth/permisos';
import { pintar, respuestaJson, usuarioDePrueba } from '../test/utilidades';
import { Rutas } from './Rutas';

const TODOS = Object.values(PERMISOS);
const ADMIN = usuarioDePrueba(TODOS, [{ codigo: 'ADMINISTRADOR', nombre: 'Administrador' }]);
const RECEPCION = usuarioDePrueba([]);

afterEach(() => vi.unstubAllGlobals());

function opcionesDelMenu(): string[] {
  const menu = screen.getByRole('navigation', { name: 'Menú principal' });
  return within(menu)
    .getAllByRole('link')
    .map((enlace) => enlace.textContent ?? '');
}

describe('rutas y menú', () => {
  it('sin sesión, cualquier página lleva a la pantalla de entrada', () => {
    pintar(<Rutas />, { ruta: '/usuarios' });
    expect(screen.getByRole('heading', { name: 'Software de gestión' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Usuario o correo/)).toBeInTheDocument();
  });

  it('el administrador ve todos los módulos en el menú', () => {
    pintar(<Rutas />, { auth: { usuario: ADMIN } });
    expect(opcionesDelMenu()).toEqual([
      'Inicio',
      'Agenda',
      'Pacientes',
      'Usuarios',
      'Roles y permisos',
      'Auditoría',
    ]);
    expect(screen.getByRole('heading', { name: 'Hola, Marta' })).toBeInTheDocument();
  });

  it('el menú solo ofrece lo que el usuario tiene permitido', () => {
    pintar(<Rutas />, { auth: { usuario: usuarioDePrueba([PERMISOS.AUDITORIA_VER]) } });
    expect(opcionesDelMenu()).toEqual(['Inicio', 'Auditoría']);
  });

  it('sin permisos, el inicio lo explica y una página prohibida no se abre aunque se escriba su dirección', () => {
    const { unmount } = pintar(<Rutas />, { auth: { usuario: RECEPCION } });
    expect(opcionesDelMenu()).toEqual(['Inicio']);
    expect(screen.getByText(/todavía no tiene acceso a ningún módulo/)).toBeInTheDocument();
    unmount();

    const fetchFalso = vi.fn<typeof fetch>();
    vi.stubGlobal('fetch', fetchFalso);
    pintar(<Rutas />, { ruta: '/usuarios', auth: { usuario: RECEPCION } });
    expect(
      screen.getByRole('heading', { name: 'No tienes permiso para ver esta página' }),
    ).toBeInTheDocument();
    // Ni siquiera se pide nada a la API (y si se pidiera, respondería 403)
    expect(fetchFalso).not.toHaveBeenCalled();
  });

  it('con permiso, la página de usuarios carga la lista de la API', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn<typeof fetch>(() =>
        Promise.resolve(
          respuestaJson({
            contenido: [
              {
                id: 1,
                username: 'admin',
                email: 'admin@example.com',
                nombre: 'Administrador',
                activo: true,
                bloqueado: false,
                creado_en: '2026-10-12T08:00:00',
                roles: [{ codigo: 'ADMINISTRADOR', nombre: 'Administrador' }],
              },
            ],
            pagina: 0,
            tamano: 20,
            total_elementos: 1,
            total_paginas: 1,
          }),
        ),
      ),
    );
    pintar(<Rutas />, { ruta: '/usuarios', auth: { usuario: ADMIN } });

    const tabla = await screen.findByRole('table', { name: 'Usuarios del software' });
    expect(within(tabla).getByText('admin@example.com')).toBeInTheDocument();
    expect(within(tabla).getByText('Nunca')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuevo usuario' })).toBeInTheDocument();
  });

  it('una dirección que no existe muestra su aviso', () => {
    pintar(<Rutas />, { ruta: '/no-existe', auth: { usuario: ADMIN } });
    expect(screen.getByRole('heading', { name: 'Esta página no existe' })).toBeInTheDocument();
  });

  it('si el servidor no responde, lo dice y deja reintentar', () => {
    pintar(<Rutas />, { auth: { estado: 'sin-conexion' } });
    expect(screen.getByRole('heading', { name: 'No se puede conectar con el servidor' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
