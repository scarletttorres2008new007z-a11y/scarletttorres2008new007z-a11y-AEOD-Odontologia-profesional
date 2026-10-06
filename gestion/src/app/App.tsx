import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { BrowserRouter } from 'react-router';
import { AuthProvider } from '../features/auth/AuthProvider';
import { crearClienteDeConsultas } from './consultas';
import { Rutas } from './Rutas';

export function App() {
  const [cliente] = useState(crearClienteDeConsultas);
  return (
    <QueryClientProvider client={cliente}>
      <AuthProvider>
        <BrowserRouter>
          <Rutas />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
