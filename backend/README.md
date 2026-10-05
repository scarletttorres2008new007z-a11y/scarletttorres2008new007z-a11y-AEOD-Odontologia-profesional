# AEOD · API de la landing

API REST de la landing de AEOD: tratamientos (con su precio "desde" y su duración), equipo, contacto
y **reserva de citas con disponibilidad real**. El backend calcula qué horarios están libres a partir del
horario de la clínica, los turnos de cada odontólogo, el almuerzo, los bloqueos y las citas ya reservadas,
e impide la doble reserva en la propia base de datos.

**No es el software de gestión de la clínica**: no hay login, usuarios, panel, CRUD de pacientes,
historia clínica, pagos ni envío de notificaciones. La lógica de agenda está en servicios propios
(`DisponibilidadService`, `CitaService`) para que el futuro software de clínica y la app del paciente la reutilicen.

Java 21 · Spring Boot 3.5 · Spring Web MVC · Spring Data JPA · Jakarta Validation · SQL Server (XAMPP/MariaDB y MySQL opcionales) · Maven

## Arrancar en local (IntelliJ + SQL Server)

1. **Base de datos.** Con SQL Server en marcha, ejecuta una vez `database/crear-base-de-datos.sql` (en SSMS o en la consola de base de datos de IntelliJ). Las tablas las crea la aplicación al arrancar.
2. **Abrir el proyecto.** En IntelliJ: *File → Open* y elige la carpeta `backend` (la que tiene `pom.xml`). IntelliJ la reconoce como proyecto Maven. Comprueba que el SDK es Java 21 (*File → Project Structure → SDK*).
3. **Usuario y contraseña.** Abre `ClinicaLandingApplication` y pulsa la flecha verde. La primera vez fallará la conexión: entra en *Run → Edit Configurations… → Environment variables* y pon
   `DB_USER=sa;DB_PASSWORD=tu_contraseña`
   (o el usuario de SQL Server que uses). Vuelve a ejecutar.
4. **Comprobar.** Abre <http://localhost:8080/api/health>.

Al primer arranque se cargan **datos de prueba**: los tratamientos, precios y equipo que ya muestra la landing, más un horario, turnos, almuerzo y duraciones **de ejemplo** (ver «Agenda»). Cámbialos por los reales antes de publicar y desactiva la carga con `app.datos-iniciales=false`. Cada bloque solo se carga si su tabla está vacía, así que una base de datos de una versión anterior se completa sola al arrancar.

Si no conecta, revisa en *SQL Server Configuration Manager* que **TCP/IP** está habilitado en el puerto **1433** y que el servidor acepta **autenticación de SQL Server** (modo mixto). Con SQL Server Express (instancia con nombre), pon su puerto en `DB_PORT` o fija 1433 en la configuración TCP/IP.

Desde terminal también funciona: `DB_PASSWORD=tu_contraseña mvn spring-boot:run`.

### Alternativa: XAMPP (MariaDB) en lugar de SQL Server

1. En el panel de XAMPP pulsa **Start** en **MySQL**.
2. Crea la base de datos una vez, ejecutando `database/crear-base-de-datos-mysql.sql` en la consola de base de datos de IntelliJ (*Database → + → Data Source → MariaDB*, usuario `root`, sin contraseña, puerto 3306) o en phpMyAdmin. La crea en `utf8mb4` para que se guarden bien tildes, ñ y cualquier carácter.
3. En *Run → Edit Configurations → Environment variables* pon solo `SPRING_PROFILES_ACTIVE=xampp` y ejecuta `ClinicaLandingApplication`.

No hace falta Tomcat aparte (ni Smart Tomcat): Spring Boot ya lleva Tomcat dentro y arranca en el puerto 8080.

Para MySQL 8 sin XAMPP existe también el perfil `mysql`.

## Ver la landing con datos de la API

La landing debe abrirse desde un servidor local (por CORS, abrir `index.html` con doble clic no puede hablar con la API).

- **Desde IntelliJ:** abre `index.html` de la carpeta raíz y pulsa el icono del navegador que aparece arriba a la derecha (abre `http://localhost:63342/...`, que ya está permitido).
- **Desde terminal:** en la carpeta raíz, `python3 -m http.server 5500` y abre <http://localhost:5500>.

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
2. La cita se guarda junto con un registro por cada tramo de 15 minutos en `agenda_ocupacion`, que tiene **clave única** (odontólogo, fecha, hora). Si dos personas confirman a la vez, la base de datos solo acepta una; la otra recibe el 409. Probado con 10 reservas simultáneas en SQL Server, MariaDB y H2.

En la consola verás entonces un `ERROR … Duplicate entry … uk_agenda_ocupacion` (MariaDB) o `Violation of UNIQUE KEY constraint 'uk_agenda_ocupacion'` (SQL Server): es la protección funcionando, no un fallo.

### Estados de una cita

| Estado | Ocupa el horario | Cuándo |
| --- | --- | --- |
| `PENDIENTE` | Sí | Reservada desde la web; la clínica la confirma |
| `CONFIRMADA` | Sí | Confirmada (o reservada con `app.citas.confirmacion-automatica=true`) |
| `CANCELADA` | No | Cancelada. **No se borra** y el horario vuelve a estar libre |
| `REPROGRAMADA` | No | Se movió a otra cita, enlazada por `cita_anterior_id` |
| `COMPLETADA`, `NO_ASISTIO` | No | Para el futuro software de clínica |

Cancelar y reprogramar existen en `CitaService` con su regla (el paciente, hasta 4 horas antes; la clínica, siempre), pero **no tienen endpoint público**: sin identificar al paciente no es seguro. Los usará la app del paciente.

### Tablas de agenda

| Tabla | Qué guarda |
| --- | --- |
| `horarios_clinica` | Apertura y cierre por día (`dia_semana` 1 = lunes … 7 = domingo). Un día sin fila está cerrado |
| `horarios_odontologo` | Turnos de cada odontólogo por día; puede haber varios tramos el mismo día |
| `odontologo_tratamientos` | Quién hace cada tratamiento. Un tratamiento sin filas lo hace cualquiera |
| `tratamientos.duracion_minutos` | Duración de la cita que se reserva online |
| `bloqueos` | Almuerzo, reuniones, mantenimiento, vacaciones, feriados y bloqueos manuales (ver abajo) |
| `citas` | Las citas, con estado, origen (`LANDING`, `CLINICA`, `APP_PACIENTE`) y código público |
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
- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`: conexión a SQL Server (por defecto `localhost:1433`, `sa`)
- Los textos se guardan como `nvarchar` (tildes, ñ y cualquier carácter)
- `app.cors.origenes`: orígenes permitidos (por defecto `localhost` y `127.0.0.1` en los puertos 5500 y 3000, más `localhost:63342` de IntelliJ; nunca `*`)
- `app.datos-iniciales`: carga los datos de prueba si las tablas están vacías
- `app.zona-horaria`: zona de la clínica para decidir qué es "hoy" (`Europe/Madrid`)
- `app.agenda.intervalo-minutos` (30): cada cuánto empieza un horario ofrecido
- `app.agenda.antelacion-minima-minutos` (60): margen mínimo para reservar hoy
- `app.agenda.dias-reserva-maximos` (180): hasta cuántos días vista se reserva
- `app.agenda.dias-busqueda-alternativas` (30): días que se exploran para las próximas opciones
- `app.agenda.inicio-tarde` (14:00): desde qué hora un horario cuenta como "tarde"
- `app.citas.confirmacion-automatica` (false): `true` = las citas de la web entran `CONFIRMADA`
- `app.citas.cancelacion-antelacion-horas` (4): antelación mínima para que el paciente cancele o reprograme

Las tablas se crean con `spring.jpa.hibernate.ddl-auto=update`, suficiente en desarrollo. Para producción conviene cambiarlo a `validate` y gestionar el esquema con scripts.

## Tests

```bash
mvn test
```

Usan una base de datos H2 en memoria, así que no necesitan SQL Server. Cubren el cálculo de disponibilidad (incluido el ejemplo 8–17 con almuerzo y una cita de 10 a 11), las reservas simultáneas, la cancelación y la reprogramación. En IntelliJ: clic derecho en `src/test/java` → *Run 'All Tests'*.

## Estructura

```text
src/main/java/sv/clinica/landing/
├── config/       CorsConfig, DataInitializer, AgendaProperties, CitasProperties
├── controller/   Health, Tratamiento, Odontologo, Contacto, Cita, Disponibilidad
├── dto/          Respuestas, peticiones con validaciones y patrones compartidos
├── entity/       Tratamiento, Odontologo, horarios, Bloqueo, Cita, OcupacionAgenda, ListaEspera, Notificacion y sus enums
├── event/        CitaEvento (reserva, cancelación, reprogramación)
├── repository/   Spring Data JPA
├── service/      DisponibilidadService (agenda), CitaService (reservar, cancelar, reprogramar), NotificacionService
└── exception/    RecursoNoEncontrado, DatosInvalidos, HorarioNoDisponible (409), CitaNoModificable (409), GlobalExceptionHandler
```
