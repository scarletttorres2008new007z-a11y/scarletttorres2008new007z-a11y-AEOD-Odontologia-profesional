# AEOD · Software de gestión

Aplicación privada para el personal de la clínica (recepción, odontólogos, coordinación y administración).

- **Tecnología:** React + TypeScript + Vite, con CSS propio (colores y tipografías de la landing).
- **Datos:** solo a través de la API central (`../backend`). Nunca se conecta a la base de datos.
- **Seguridad:** entrada con usuario y contraseña. El token vive solo en memoria y se renueva con una cookie `HttpOnly`.
  El menú oculta lo que no está permitido, pero quien decide es el backend: sin permiso responde 403.
- **Fase 1:** entrada, menú lateral y cabecera, inicio, usuarios, roles y permisos, auditoría y «Mi cuenta». Los módulos
  siguientes llegan fase a fase: pacientes, agenda y citas, área clínica, administración, comunicación y reportes.

## Arrancar

Con el backend en marcha en `http://localhost:8080` (ver `../COMO-ABRIR.md`):

```bash
npm install     # solo la primera vez
npm run dev     # http://localhost:5173
```

Vite pasa todo lo que empieza por `/api` al backend, así el navegador ve el software y la API en el mismo sitio. Para
usar otra dirección: `AEOD_API=http://otra:8080 npm run dev`.

## Comandos

| Comando | Qué hace |
| --- | --- |
| `npm run lint` | Revisa el código con oxlint |
| `npm run format` | Ordena el formato con Prettier (`format:check` solo comprueba) |
| `npm run typecheck` | Comprueba los tipos |
| `npm test` | Pruebas con Vitest y Testing Library |
| `npm run build` | Versión para publicar en `dist/` |
| `npm run api:tipos` | Vuelve a generar `src/shared/api/esquema.ts` desde la API (`/v3/api-docs`) con el backend encendido |

## Estructura

```text
src/
├── app/        App, rutas, guardas de sesión y permiso, estructura (cabecera y menú)
├── features/   auth, inicio, usuarios, roles, auditoria, cuenta (cada una con su api.ts y sus pantallas)
├── shared/     cliente de la API, tipos generados, componentes (botones, campos, diálogos…), estilos
└── test/       preparación y utilidades de las pruebas
```
