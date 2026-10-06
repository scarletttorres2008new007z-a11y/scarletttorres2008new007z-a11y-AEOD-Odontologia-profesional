import type { ReactNode } from 'react';

// Iconos de línea propios (24×24). Son decorativos: el texto de al lado dice lo que hace cada opción.
const TRAZOS = {
  inicio: <path d="M3.5 10.5 12 3.5l8.5 7V20a1 1 0 0 1-1 1h-5v-6h-5v6h-5a1 1 0 0 1-1-1z" />,
  usuarios: (
    <>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6" />
      <path d="M16 4.6a3.5 3.5 0 0 1 0 6.8" />
      <path d="M18.2 14.3c2 .8 3.3 2.9 3.3 5.7" />
    </>
  ),
  roles: (
    <>
      <path d="M12 3 4.5 6v5.5c0 4.6 3.2 8.3 7.5 9.5 4.3-1.2 7.5-4.9 7.5-9.5V6z" />
      <path d="m9 12 2 2 4-4" />
    </>
  ),
  auditoria: (
    <>
      <rect x="5" y="4" width="14" height="17" rx="2" />
      <path d="M9 2.5h6v3H9z" />
      <path d="M9 10h6M9 14h6M9 18h3" />
    </>
  ),
  pacientes: (
    <>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <circle cx="9" cy="10.5" r="2.25" />
      <path d="M5.75 16c.55-1.5 1.75-2.35 3.25-2.35s2.7.85 3.25 2.35" />
      <path d="M15 10h3.5M15 13.5h3.5" />
    </>
  ),
  agenda: (
    <>
      <rect x="3.5" y="5" width="17" height="15.5" rx="2" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
      <path d="M7.5 14h2.5M13.5 14h3M7.5 17.5h2.5" />
    </>
  ),
  odontologos: (
    <>
      <circle cx="10" cy="7.5" r="3.5" />
      <path d="M3.5 20.5c0-3.6 2.9-6.5 6.5-6.5 1.3 0 2.5.4 3.5 1" />
      <path d="M18 14.5v6M15 17.5h6" />
    </>
  ),
  tratamientos: (
    <path d="M7.5 3.5c-2.3 0-4 1.8-4 4.3 0 2.5 1.2 4.1 1.8 6.2.5 1.9.6 4.1 1.4 5.8.4.9 1.6.9 2 0 .7-1.6.9-4.5 3.3-4.5s2.6 2.9 3.3 4.5c.4.9 1.6.9 2 0 .8-1.7.9-3.9 1.4-5.8.6-2.1 1.8-3.7 1.8-6.2 0-2.5-1.7-4.3-4-4.3-1.8 0-2.8 1.1-4.5 1.1s-2.7-1.1-4.5-1.1z" />
  ),
  horarios: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 7.5V12l3 2" />
    </>
  ),
  bloqueos: (
    <>
      <rect x="3.5" y="5" width="17" height="15.5" rx="2" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
      <path d="m10 13.5 4 4m0-4-4 4" />
    </>
  ),
  cuenta: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </>
  ),
  salir: (
    <>
      <path d="M9 21H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3" />
      <path d="m16 17 5-5-5-5" />
      <path d="M21 12H9" />
    </>
  ),
  menu: <path d="M4 6h16M4 12h16M4 18h16" />,
  cerrar: <path d="M6 6l12 12M18 6 6 18" />,
  flecha: <path d="M5 12h14m-6-6 6 6-6 6" />,
  volver: <path d="M19 12H5m6-6-6 6 6 6" />,
  anterior: <path d="m15 6-6 6 6 6" />,
  siguiente: <path d="m9 6 6 6-6 6" />,
} satisfies Record<string, ReactNode>;

export type NombreIcono = keyof typeof TRAZOS;

export function Icono({ nombre, tamano = 20 }: { nombre: NombreIcono; tamano?: number }) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {TRAZOS[nombre]}
    </svg>
  );
}
