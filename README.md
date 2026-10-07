# AEOD · Odontología Profesional

Sistema de la clínica en un solo repositorio, con proyectos separados que comparten **una API central y una base de
datos central**. La información pertenece al sistema central: la landing, el software de gestión y la futura app del
paciente son clientes de la misma API.

| Carpeta | Qué es | Tecnología | Estado |
| --- | --- | --- | --- |
| [`landing/`](landing/README.md) | Web pública: tratamientos, precios, equipo y reserva de citas | HTML, CSS y JavaScript | En uso |
| [`backend/`](backend/README.md) | API REST central: reglas de negocio, disponibilidad, citas y acceso a la base de datos | Java 21, Spring Boot, Flyway | En uso |
| [`gestion/`](gestion/README.md) | Software de gestión para recepción, odontólogos y administración | React, TypeScript, Vite | Fase 3: entrada, usuarios, roles, auditoría, pacientes, agenda, citas y configuración de la agenda |

```text
landing/  ──┐
            ├──>  backend/ (API REST /api)  ──>  base de datos central clinica_aeod
gestion/  ──┘                                     MySQL 8.4 en producción · MariaDB de XAMPP en desarrollo
```

## Reglas del sistema

- **Una sola base de datos y una sola tabla de citas.** Una cita creada en la landing es el mismo registro que ve
  recepción en el software, sin copias ni sincronizaciones.
- **Ningún cliente se conecta a la base de datos.** La landing y el software hablan solo con la API.
- **La lógica vive en el backend.** La disponibilidad, las duraciones, la prevención de doble reserva y las validaciones
  se calculan en el servidor; los clientes solo muestran lo que reciben.
- **La base de datos evoluciona con migraciones** (Flyway, `backend/src/main/resources/db/migration`), nunca a mano.
- **Ningún secreto en el código.** Contraseñas y URLs de producción llegan por variables de entorno.

## Empezar

- Arrancar todo en tu equipo (XAMPP + IntelliJ + VS Code): [COMO-ABRIR.md](COMO-ABRIR.md)
- API, entornos (`dev`, `test`, `prod`), variables de entorno y migraciones: [backend/README.md](backend/README.md)
- Landing: [landing/README.md](landing/README.md)

## Pruebas automáticas

GitHub Actions compila el backend y pasa sus pruebas en MySQL 8.4 y en MariaDB 10.4 en cada cambio
(`.github/workflows/backend.yml`), y revisa, prueba y compila el software de gestión (`.github/workflows/gestion.yml`).
