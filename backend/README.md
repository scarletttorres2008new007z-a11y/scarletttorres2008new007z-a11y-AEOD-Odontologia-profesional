# AEOD · API central

API REST central de AEOD: tratamientos (con su precio "desde" y su duración), equipo, contacto
y **reserva de citas con disponibilidad real**. El backend calcula qué horarios están libres a partir del
horario de la clínica, los turnos de cada odontólogo, el almuerzo, los bloqueos y las citas ya reservadas,
e impide la doble reserva en la propia base de datos.

Hoy la usa la landing (`../landing`). El software de gestión (`../gestion`, desde la Fase 1) y la futura app del paciente
usarán esta misma API y la misma base de datos: **una sola base, una sola tabla de citas**. Ningún cliente se conecta
a la base directamente.

Java 21 · Spring Boot 3.5 · Spring Web MVC · Spring Data JPA · Jakarta Validation · Flyway · MySQL 8.4 / MariaDB (XAMPP) · Maven

## Arrancar en local (IntelliJ + XAMPP)

La guía paso a paso está en [`../COMO-ABRIR.md`](../COMO-ABRIR.md). En resumen:

1. En el panel de XAMPP pulsa **Start** en **MySQL** (es MariaDB, usuario `root` sin contraseña).
2. Abre la carpeta del repositorio en IntelliJ, carga `backend/pom.xml` como proyecto Maven y usa Java 21.
3. Ejecuta `ClinicaApiApplication`. **No hace falta ninguna variable de entorno ni ningún script SQL**: sin perfil
   arranca en `dev`, crea la base `clinica_aeod`, Flyway crea las tablas y se cargan los datos de ejemplo.
4. Comprueba <http://localhost:8080/api/health>.

No hace falta Tomcat aparte (ni Smart Tomcat): Spring Boot ya lleva Tomcat dentro y arranca en el puerto 8080.

**Con MySQL 8.4 en lugar de XAMPP**, pon en *Run → Edit Configurations… → Environment variables*:
`DB_URL=jdbc:mysql://localhost:3306/clinica_aeod?createDatabaseIfNotExist=true;DB_PASSWORD=tu_contraseña`.

## Entornos

El entorno se elige con `SPRING_PROFILES_ACTIVE`. Sin esa variable se usa `dev`.

| Perfil | Dónde | Base de datos | Datos de ejemplo |
| --- | --- | --- | --- |
| `dev` | Tu equipo | Por defecto XAMPP: `jdbc:mariadb://localhost:3306/clinica_aeod`, `root` sin contraseña | Sí |
| `test` | Servidor de pruebas | Por variables de entorno, obligatorias | Sí |
| `prod` | Producción | Por variables de entorno, obligatorias (MySQL 8.4) | No |

Variables de entorno (en `test` y `prod` son obligatorias; si falta alguna, el backend no arranca y dice cuál):

| Variable | Ejemplo | Para qué |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://db.ejemplo.com:3306/clinica_aeod` | Conexión. `jdbc:mysql://` para MySQL, `jdbc:mariadb://` para MariaDB |
| `DB_USERNAME` | `clinica_app` | Usuario de la base |
| `DB_PASSWORD` | — | Contraseña. Nunca se escribe en el código ni se sube al repositorio |
| `CORS_ALLOWED_ORIGINS` | `https://www.ejemplo.com` | Webs que pueden llamar a las rutas **públicas** de la API desde el navegador (la landing). Nunca `*` |
| `JWT_SECRET` | 64 caracteres aleatorios | Firma de los tokens de sesión (mínimo 32 bytes). En `dev`, si falta, se usa una clave temporal |
| `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | — | Solo para crear o recuperar el administrador. Se quitan después de arrancar |

## Seguridad (Fase 1)

- **Login** en `POST /api/auth/login` (usuario o correo + contraseña). Devuelve un token de acceso JWT (15 minutos) y
  deja una cookie `HttpOnly`, `SameSite=Strict`, con la que el software lo renueva (`POST /api/auth/refresh`) sin guardar
  nada en `localStorage`. `POST /api/auth/logout` anula la sesión en la base de datos.
- **Contraseñas** con BCrypt; mínimo 10 caracteres. Cinco fallos seguidos bloquean el usuario 15 minutos (423).
- **Permisos (RBAC)**: cada endpoint del software exige su permiso con `@PreAuthorize`; sin él, **403**, aunque se llame
  directamente. Los permisos se leen de la base en cada petición: un cambio de rol o una desactivación valen al momento.
- **Límite de peticiones** por IP (429): entrada 10/min, reservas de la web 20/h, mensajes de contacto 20/h.
- **CORS** solo en las rutas públicas de la landing; el software usa el mismo origen (proxy de Vite en desarrollo).
- **Cabeceras**: `Content-Security-Policy`, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, `no-store` y HSTS
  por HTTPS. Detrás de un proxy en producción, `server.forward-headers-strategy=native`.
- **Auditoría** (`/api/auditoria`, permiso `auditoria.ver`): entradas, fallos, bloqueos, cambios de usuarios, roles y
  permisos, altas y cambios de pacientes, y reservas, cancelaciones y cambios de citas (sin datos del paciente).
- **Primer administrador**: en `dev`, si no hay ninguno, se crea `admin` con una contraseña aleatoria que se muestra una
  vez en la consola. En `test` y `prod`, con `ADMIN_USERNAME`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`.
- **Swagger**: <http://localhost:8080/swagger-ui.html> en `dev` y `test` (desactivado en `prod`).
- **Errores**: siempre `{ timestamp, status, error, message, path }` (+ `errores` por campo), desde un único manejador.

## Pacientes (Fase 2)

Solo datos personales, de contacto y administrativos (tabla `pacientes`, migración `V3__pacientes.sql`). Los datos
clínicos irán en el expediente, en otra tabla.

| Método | Ruta | Permiso | Qué hace |
| --- | --- | --- | --- |
| GET | `/api/pacientes?texto=&activo=&pagina=0&tamano=20` | `pacientes.ver` | Busca por palabras del nombre, los apellidos, el documento, el teléfono, el correo o el código, sin distinguir mayúsculas ni tildes. Ordenados por apellidos; máximo 100 por página |
| GET | `/api/pacientes/{id}` | `pacientes.ver` | Ficha del paciente (404 si no existe) |
| POST | `/api/pacientes` | `pacientes.crear` | Alta. 201; 400 con errores por campo; **409** si el documento ya existe |
| PUT | `/api/pacientes/{id}` | `pacientes.editar` | Sustituye sus datos |
| PUT | `/api/pacientes/{id}/estado` | `pacientes.editar` | `{ "activo": false }` da de baja (no borra nada); `true` lo reactiva |

- **Validaciones en el backend:** nombre, apellidos y teléfono obligatorios; DNI y NIE con su letra de control; el tipo
  y el número de documento van juntos; teléfono de 9 a 15 dígitos (con `+` opcional); correo válido; nacimiento no
  futuro. Se guardan normalizados: documento en mayúsculas sin guiones, teléfono solo con `+` y dígitos, correo en
  minúsculas. La base también impide documentos repetidos (`uk_paciente_documento`).
- **Código del paciente:** 6 caracteres aleatorios sin vocales ni caracteres confundibles (p. ej. `K7M3QX`), generado
  por el backend; no se puede elegir ni cambiar.
- **Auditoría:** el alta guarda todos los datos; una edición guarda solo los campos que cambiaron (antes y después);
  guardar sin cambios no deja registro.
- **Permisos iniciales** (se cambian en Roles y permisos): Administrador, Recepción y Coordinador ven, dan de alta y
  editan; Odontólogo solo ve.

## Agenda y citas en el software (Fase 3, parte 1)

Una sola tabla `citas` para la web, el software y la futura app (migración `V4__agenda_y_citas.sql`). Una cita del
software ocupa el horario igual que una de la web, y al cancelarla el hueco vuelve a salir en la landing.

| Método | Ruta | Permiso | Qué hace |
| --- | --- | --- | --- |
| GET | `/api/agenda?desde=2026-10-12&dias=1..7[&odontologo_id=]` | `citas.ver` | Días con horario de la clínica, turnos, bloqueos y citas |
| GET | `/api/agenda/disponibilidad?tratamiento_id=&fecha=[&odontologo_id=][&excluir_cita_id=]` | `citas.crear` o `citas.reprogramar` | Huecos libres de cada odontólogo, calculados como los de la web |
| GET | `/api/citas?desde=&hasta=&estado=&origen=&odontologo_id=&paciente_id=&sin_ficha=&recientes=&pagina=&tamano=` | `citas.ver` | Busca citas |
| GET | `/api/citas/{id}` | `citas.ver` | Ficha de la cita con su historia; **404** si es de otra agenda |
| POST | `/api/pacientes/{id}/citas` | `citas.crear` | Da una cita a un paciente activo (origen `SOFTWARE`, entra `CONFIRMADA`); **409** si el hueco ya no está libre |
| PUT | `/api/citas/{id}/estado` | `citas.cambiar_estado` | Confirmar, en consulta, completada, no asistió (solo los pasos permitidos) |
| POST | `/api/citas/{id}/cancelacion` | `citas.cancelar` | `{ "motivo": "…" }` opcional. No se borra: queda `CANCELADA` y el hueco se libera |
| POST | `/api/citas/{id}/reprogramacion` | `citas.reprogramar` | Crea la cita nueva y deja la anterior `REPROGRAMADA`, enlazadas |
| PUT | `/api/citas/{id}/paciente` | `citas.editar` | Vincula una cita de la web a la ficha de un paciente |
| PUT | `/api/citas/{id}/notas` | `citas.editar` | Notas internas (el paciente no las ve) |
| POST | `/api/citas/cancelacion` | Público | El paciente cancela desde la web con el `codigo` de su reserva, hasta 4 horas antes |

- **Quién ve qué:** con `citas.ver_todas` se ve la agenda de todos; sin él, solo la del odontólogo vinculado al usuario
  (`odontologos.usuario_id`). Pedir una cita de otra agenda responde 404, no 403, para no revelar que existe.
- **Validaciones en el backend:** paciente activo, tratamiento activo, odontólogo activo que hace ese tratamiento, fecha
  no pasada y como mucho a 180 días, hora dentro de su turno y del horario de la clínica, fuera de almuerzo y bloqueos,
  sin choque con otra cita, y el paso de estado permitido. La duración sale siempre de `tratamientos.duracion_minutos`.
- **Auditoría:** reservar, cambiar de estado, cancelar (con el motivo), reprogramar y vincular, con antes y después.
- **Permisos iniciales** (se cambian en Roles y permisos): Administrador, Recepción y Coordinador, todos los `citas.*`;
  Odontólogo, solo `citas.ver` (su propia agenda).
- **Pendiente para la parte 2:** pantallas de odontólogos, tratamientos y duraciones, horarios y bloqueos, y vincular un
  usuario a su odontólogo. Hasta entonces se cambian en la base de datos (ver **Agenda**, más abajo).

## Base de datos y migraciones

Las migraciones se aplican solas al arrancar, así que el usuario de `DB_USERNAME` necesita permiso para crear y modificar
tablas en su base. Antes de desplegar una versión con migraciones nuevas en producción, haz una copia de seguridad de la base.

- **Motores:** MySQL 8.4 en producción y la MariaDB 10.4 de XAMPP en desarrollo. Las migraciones se escriben en SQL que
  funciona en los dos, y GitHub Actions pasa las pruebas en ambos en cada cambio (`.github/workflows/backend.yml`).
- **Flyway** crea y cambia las tablas con los archivos de `src/main/resources/db/migration`:
  `V1__esquema_inicial.sql`, después `V2__...sql`, `V3__...sql`… Cada archivo se aplica una sola vez y queda anotado en la
  tabla `flyway_schema_history`.
- **Hibernate no toca la estructura** (`spring.jpa.hibernate.ddl-auto=validate`): al arrancar comprueba que las entidades
  coinciden con las tablas y, si no, el backend no arranca.
- **Reglas para cambiar la base:** un cambio de estructura es siempre un archivo nuevo `V<n>__descripcion.sql`; nunca se
  edita una migración ya aplicada ni se cambian tablas a mano desde Workbench o phpMyAdmin. Cada `CREATE TABLE` lleva
  `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` (válido en MySQL 8.4 y en MariaDB). Para añadir una
  columna obligatoria a una tabla con datos: primero opcional, luego se rellena y después se hace obligatoria.
- **Borrar la base con Flyway (`clean`) está desactivado** en todos los entornos; solo lo usan las pruebas automáticas
  sobre su propia base.

La base anterior de la landing (`clinica_landing`) no se usa ni se modifica. La base central es `clinica_aeod`.

## Ver la landing con datos de la API

La landing debe abrirse desde un servidor local (por CORS, abrir `index.html` con doble clic no puede hablar con la API).

- **Desde IntelliJ:** abre `landing/index.html` y pulsa el icono del navegador que aparece arriba a la derecha (abre `http://localhost:63342/...`, que ya está permitido).
- **Desde VS Code:** con la extensión Live Server, abre `landing/index.html` con *Open with Live Server* (puerto 5500, permitido).

Si la API está apagada, la landing sigue funcionando con sus datos estáticos y el formulario muestra el teléfono en vez de fingir un envío.

## Endpoints

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | `/api/health` | 200 `{ "success": true, "message": "Backend funcionando correctamente." }` |
| GET | `/api/tratamientos` | 200, tratamientos activos con `duracion_minutos` y `odontologo_ids` (vacío = cualquiera) |
| GET | `/api/tratamientos/{id}` | 200, o 404 si no existe o está inactivo |
| GET | `/api/odontologos` | 200, odontólogos activos ordenados |
| GET | `/api/odontologos/{id}` | 200, o 404 |
| GET | `/api/disponibilidad?tratamiento_id=2&fecha=2026-10-15[&odontologo_id=3][&franja=MANANA\|TARDE]` | 200, horarios libres del día |
| GET | `/api/disponibilidad/proximos?tratamiento_id=2[&desde=…][&odontologo_id=…][&franja=…][&limite=6]` | 200, los horarios libres más cercanos |
| POST | `/api/contacto` | 201, o 400 con errores por campo |
| POST | `/api/citas` | 201 con la cita, 400 con errores por campo, **409 si el horario ya no está libre** |

Los campos JSON van en `snake_case`.

### Disponibilidad

```json
{
  "fecha": "2026-10-13",
  "tratamiento": { "id": 2, "nombre": "Limpieza dental", "duracion_minutos": 60 },
  "horarios": [
    { "fecha": "2026-10-13", "hora_inicio": "09:00", "hora_fin": "10:00",
      "odontologo_id": 1, "odontologo_nombre": "Dra. Rocío Fernández", "odontologo_especialidad": "…" }
  ]
}
```

Si ese día no hay ningún horario, `horarios` llega vacío con `"mensaje": "No encontramos disponibilidad para el horario seleccionado."` y `proximas_opciones` (hasta 6, como mucho 3 por día y separadas al menos una hora). Sin `odontologo_id`, cada hora sale una sola vez, con el odontólogo que menos citas tiene ese día.

### Reservar

```json
{
  "tratamiento_id": 2,
  "odontologo_id": 1,
  "fecha": "2026-10-13",
  "hora_inicio": "09:00",
  "nombre": "Lucía Gómez",
  "telefono": "600 123 456",
  "email": "lucia@example.com",
  "mensaje": "Opcional"
}
```

Respuesta 201:

```json
{
  "success": true,
  "message": "Tu cita está reservada. Te llamaremos para confirmarla.",
  "datos": { "codigo": "b8da44f7-…", "estado": "PENDIENTE", "fecha": "2026-10-13",
             "hora_inicio": "09:00", "hora_fin": "10:00",
             "tratamiento": "Limpieza dental", "odontologo": "Dra. Rocío Fernández" }
}
```

Si otra persona reservó ese horario antes (o no estaba entre los ofrecidos), responde **409** con `"message": "Este horario acaba de ser reservado. Selecciona otra opción."`. La respuesta nunca incluye el id interno: solo el `codigo` público.

Error de validación (400):

```json
{
  "success": false,
  "message": "Revisa los datos enviados.",
  "errores": { "fecha": "La fecha no puede ser anterior a hoy." }
}
```

Reglas de los datos: nombre (2–100, letras), teléfono (9–15 dígitos, admite `+`), email válido, mensaje hasta 1000 caracteres; tratamiento y odontólogo existentes y activos, el odontólogo tiene que hacer ese tratamiento, y la fecha entre hoy (hora de Madrid) y 180 días vista. Los errores inesperados devuelven 500 con un mensaje genérico, nunca la traza.

## Agenda

### Cómo se calcula un horario libre

Para cada odontólogo que hace el tratamiento: **su turno ∩ horario de la clínica − almuerzo y bloqueos − citas activas**. Dentro de lo que queda se buscan bloques **continuos** del largo del tratamiento, empezando cada 30 minutos. Hoy se exige además 60 minutos de antelación. El navegador solo muestra lo que devuelve la API.

### Doble reserva

1. Al confirmar, el backend vuelve a calcular si ese horario sigue libre.
2. La cita se guarda junto con un registro por cada tramo de 15 minutos en `agenda_ocupacion`, que tiene **clave única** (odontólogo, fecha, hora). Si dos personas confirman a la vez, la base de datos solo acepta una; la otra recibe el 409. Las pruebas lo comprueban con 10 reservas simultáneas en MySQL 8.4 y en MariaDB 10.4.

En la consola verás entonces un `ERROR … Duplicate entry … for key 'uk_agenda_ocupacion'`: es la protección funcionando, no un fallo.

### Estados de una cita

| Estado | Ocupa el horario | Cuándo |
| --- | --- | --- |
| `PENDIENTE` | Sí | Reservada desde la web; la clínica la confirma |
| `CONFIRMADA` | Sí | Confirmada (o reservada con `app.citas.confirmacion-automatica=true`) |
| `EN_ATENCION` | Sí | El paciente está en consulta |
| `COMPLETADA` | No | Consulta terminada |
| `NO_ASISTIO` | No | No vino (se puede deshacer y volver a `CONFIRMADA`) |
| `CANCELADA` | No | Cancelada. **No se borra** y el horario vuelve a estar libre |
| `REPROGRAMADA` | No | Se movió a otra cita, enlazada por `cita_anterior_id` |

El paciente puede cancelar desde la web con el código de su reserva, hasta 4 horas antes (`POST /api/citas/cancelacion`).
La clínica cancela y reprograma desde el software en cualquier momento. Reprogramar como paciente queda para la app.

### Tablas de agenda

| Tabla | Qué guarda |
| --- | --- |
| `horarios_clinica` | Apertura y cierre por día (`dia_semana` 1 = lunes … 7 = domingo). Un día sin fila está cerrado |
| `horarios_odontologo` | Turnos de cada odontólogo por día; puede haber varios tramos el mismo día |
| `odontologo_tratamientos` | Quién hace cada tratamiento. Un tratamiento sin filas lo hace cualquiera |
| `tratamientos.duracion_minutos` | Duración de la cita que se reserva online |
| `bloqueos` | Almuerzo, reuniones, mantenimiento, vacaciones, feriados y bloqueos manuales (ver abajo) |
| `citas` | Las citas, con estado, origen (`LANDING`, `SOFTWARE`, `APP`), código público y, si ya está vinculada, `paciente_id` |
| `agenda_ocupacion` | Tramos de 15 minutos ocupados; su clave única impide la doble reserva |
| `lista_espera` | Preparada para la futura lista de espera (sin pantalla todavía) |
| `notificaciones` | Avisos pendientes de cada reserva, cancelación, reprogramación y hueco liberado. **No se envía nada** |

Un **bloqueo** puede ser de toda la clínica (`odontologo_id` vacío) o de un odontólogo; semanal (`dia_semana`) o entre fechas (`fecha_inicio`–`fecha_fin`); de unas horas (`hora_inicio`–`hora_fin`) o del día entero (horas vacías). Ejemplos:

```sql
-- Feriado: toda la clínica cerrada el 12 de octubre
INSERT INTO bloqueos (tipo, motivo, fecha_inicio, fecha_fin, activo) VALUES ('FERIADO', 'Fiesta Nacional', '2026-10-12', '2026-10-12', 1);
-- Vacaciones de la Dra. Ana (id 3) del 1 al 15 de agosto
INSERT INTO bloqueos (tipo, motivo, odontologo_id, fecha_inicio, fecha_fin, activo) VALUES ('VACACIONES', 'Vacaciones', 3, '2027-08-01', '2027-08-15', 1);
-- Reunión de equipo todos los miércoles de 9:00 a 10:00
INSERT INTO bloqueos (tipo, motivo, dia_semana, hora_inicio, hora_fin, activo) VALUES ('REUNION', 'Reunión de equipo', 3, '09:00', '10:00', 1);
```

### Datos de ejemplo (a confirmar)

Horario de la clínica: L–V 9:00–21:00, S 10:00–14:00, D cerrado (el que muestra la landing). Almuerzo L–V 12:00–13:00.

| Odontólogo | L–V | Sábado |
| --- | --- | --- |
| Dra. Rocío Fernández | 9:00–17:00 | — |
| Dr. Marcos Ortega | 13:00–21:00 | — |
| Dra. Ana Villar | 9:00–13:00 | 10:00–14:00 |
| Dr. Carlos Méndez | 11:00–17:00 | 10:00–14:00 |

Duraciones: Valoración 30 (cualquiera) · Limpieza 60 (Ana, Rocío) · Empaste 45 (Ana, Rocío, Carlos) · Endodoncia 90 (Rocío, Ana) · Extracción 30 (Ana, Rocío) · Implante 120 (Rocío) · Invisalign Lite/Full y Brackets 60 (Marcos) · Carilla 60 (Carlos) · Blanqueamiento 90 (Carlos).

## Configuración (`src/main/resources/application.properties`)

- `server.port=8080`
- Base de datos y CORS: ver «Entornos»
- `app.zona-horaria`: zona de la clínica para decidir qué es "hoy" (`Europe/Madrid`)
- `app.datos-iniciales`: carga los datos de ejemplo si las tablas están vacías (activado en `dev` y `test`)
- `app.agenda.intervalo-minutos` (30): cada cuánto empieza un horario ofrecido
- `app.agenda.antelacion-minima-minutos` (60): margen mínimo para reservar hoy
- `app.agenda.dias-reserva-maximos` (180): hasta cuántos días vista se reserva
- `app.agenda.dias-busqueda-alternativas` (30): días que se exploran para las próximas opciones
- `app.agenda.inicio-tarde` (14:00): desde qué hora un horario cuenta como "tarde"
- `app.citas.confirmacion-automatica` (false): las citas de la web entran `PENDIENTE` hasta que la clínica las verifica; `true` = entran `CONFIRMADA`
- `app.citas.cancelacion-antelacion-horas` (4): antelación mínima para que el paciente cancele o reprograme

## Tests

```bash
mvn test
```

Usan una base de datos real y **solo para pruebas**: al empezar la borran y la crean de nuevo con las migraciones, así que
también comprueban que las migraciones funcionan. Por seguridad, solo borran una base cuyo nombre contenga «prueba» o «test».

- **En tu equipo:** con XAMPP encendido usan la base `clinica_aeod_pruebas` (se crea sola). En IntelliJ: clic derecho en
  `src/test/java` → *Run 'All Tests'*.
- **En GitHub Actions:** se ejecutan en cada cambio en MySQL 8.4 y en MariaDB 10.4. Otra base se indica con
  `TEST_DB_URL`, `TEST_DB_USERNAME` y `TEST_DB_PASSWORD`.

Cubren el cálculo de disponibilidad (incluido el ejemplo 8–17 con almuerzo y una cita de 10 a 11), las reservas
simultáneas, la cancelación y la reprogramación, la entrada y los permisos, y los pacientes (validaciones, documento
repetido, búsqueda, permisos por rol y auditoría).

## Estructura

```text
src/main/java/sv/clinica/api/
├── config/       DataInitializer, AdministradorInicial, OpenApiConfig, propiedades, VariablesDeEntornoObligatorias
├── security/     SeguridadConfig, TokenService, ConvertidorJwt, CookieDeSesion, límite de peticiones, Permisos
├── controller/   Health, Tratamiento, Odontologo, Contacto, Cita, Disponibilidad, Auth, Usuario, Rol, Auditoria, Paciente
├── dto/          Respuestas, peticiones con validaciones y patrones compartidos
├── entity/       Tratamiento, Odontologo, horarios, Bloqueo, Cita, OcupacionAgenda, ListaEspera, Notificacion, Usuario,
│                 Rol, Permiso, Sesion, RegistroAuditoria, Paciente y sus enums
├── event/        CitaEvento (reserva, cancelación, reprogramación)
├── repository/   Spring Data JPA
├── service/      DisponibilidadService (agenda), CitaService (reservar, cancelar, reprogramar), NotificacionService,
│                 AuthService, UsuarioService, RolService, AuditoriaService, PacienteService (+ BusquedaDePacientes)
└── exception/    RecursoNoEncontrado, DatosInvalidos, HorarioNoDisponible (409), CitaNoModificable (409), GlobalExceptionHandler
src/main/resources/
├── application.properties            Configuración común
├── application-dev.properties        Tu equipo (XAMPP por defecto)
├── application-test.properties       Servidor de pruebas
├── application-prod.properties       Producción
└── db/migration/                     Migraciones Flyway (V1__esquema_inicial.sql…)
```

Desde la Fase 1 el proyecto se llama `clinica-api` (paquete `sv.clinica.api`).
