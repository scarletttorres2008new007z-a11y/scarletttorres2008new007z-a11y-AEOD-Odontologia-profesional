import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../shared/api/errores';
import { pintar } from '../../test/utilidades';
import type { Auth } from './contexto';
import { PaginaEntrar } from './PaginaEntrar';

describe('pantalla de entrada', () => {
  it('pide los datos que faltan sin llamar a la API', async () => {
    const { auth } = pintar(<PaginaEntrar />);

    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(screen.getByText('Escribe tu usuario o tu correo.')).toBeInTheDocument();
    expect(screen.getByText('Escribe tu contraseña.')).toBeInTheDocument();
    expect(screen.getByLabelText(/Usuario o correo/)).toHaveFocus();
    expect(auth.entrar).not.toHaveBeenCalled();
  });

  it('entra con el usuario y la contraseña escritos', async () => {
    const { auth } = pintar(<PaginaEntrar />);

    await userEvent.type(screen.getByLabelText(/Usuario o correo/), '  marta ');
    await userEvent.type(screen.getByLabelText(/^Contraseña/), 'Clave-de-prueba-2026');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(auth.entrar).toHaveBeenCalledWith('marta', 'Clave-de-prueba-2026');
  });

  it('muestra el motivo que da la API', async () => {
    const entrar = vi.fn<Auth['entrar']>(() =>
      Promise.reject(new ApiError(401, 'Usuario o contraseña incorrectos.')),
    );
    pintar(<PaginaEntrar />, { auth: { entrar } });

    await userEvent.type(screen.getByLabelText(/Usuario o correo/), 'marta');
    await userEvent.type(screen.getByLabelText(/^Contraseña/), 'otra-clave-123');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuario o contraseña incorrectos.');
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled();
  });

  it('la contraseña se puede mostrar mientras se escribe', async () => {
    pintar(<PaginaEntrar />);
    const campo = screen.getByLabelText(/^Contraseña/);
    expect(campo).toHaveAttribute('type', 'password');

    await userEvent.click(screen.getByRole('button', { name: 'Mostrar la contraseña' }));

    expect(campo).toHaveAttribute('type', 'text');
  });

  it('avisa si la sesión terminó sola', () => {
    pintar(<PaginaEntrar />, { auth: { sesionTerminada: true } });
    expect(screen.getByRole('status')).toHaveTextContent('Tu sesión ha terminado.');
  });
});
