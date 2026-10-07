/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// Dónde está la API central (el backend Spring Boot). Vite le pasa todo lo que empieza por /api, así el
// navegador ve el software y la API en el mismo sitio: sin CORS y con la cookie de sesión protegida.
const API = process.env.AEOD_API ?? 'http://localhost:8080';
const proxy = { '/api': { target: API } };

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true, proxy },
  preview: { port: 4173, strictPort: true, proxy },
  build: { sourcemap: false },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/preparar.ts'],
    css: { modules: { classNameStrategy: 'non-scoped' } },
  },
});
