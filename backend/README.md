# AEOD · API de la landing

API REST pequeña para que la landing de AEOD tenga datos dinámicos y formularios funcionales.
Solo cubre tratamientos (con su precio "desde"), equipo, solicitudes de contacto y solicitudes de cita.
**No es un sistema de gestión clínica**: no hay usuarios, pacientes, agenda, pagos ni panel.

Java 21 · Spring Boot 3.5 · Spring Web MVC · Spring Data JPA · Jakarta Validation · SQL Server (MySQL opcional) · Maven

## Arrancar en local (IntelliJ + SQL Server)

1. **Base de datos.** Con SQL Server en marcha, ejecuta una vez `database/crear-base-de-datos.sql` (en SSMS o en la consola de base de datos de IntelliJ). Las tablas las crea la aplicación al arrancar.
2. **Abrir el proyecto.** En IntelliJ: *File → Open* y elige la carpeta `backend` (la que tiene `pom.xml`). IntelliJ la reconoce como proyecto Maven. Comprueba que el SDK es Java 21 (*File → Project Structure → SDK*).
3. **Usuario y contraseña.** Abre `ClinicaLandingApplication` y pulsa la flecha verde. La primera vez fallará la conexión: entra en *Run → Edit Configurations… → Environment variables* y pon
   `DB_USER=sa;DB_PASSWORD=tu_contraseña`
   (o el usuario de SQL Server que uses). Vuelve a ejecutar.
4. **Comprobar.** Abre <http://localhost:8080/api/health>.

Al primer arranque se cargan **datos de prueba** (los tratamientos, precios y equipo que ya muestra la landing). Son de ejemplo: cámbialos por los reales antes de publicar y desactiva la carga con `app.datos-iniciales=false`.

Si no conecta, revisa en *SQL Server Configuration Manager* que **TCP/IP** está habilitado en el puerto **1433** y que el servidor acepta **autenticación de SQL Server** (modo mixto). Con SQL Server Express (instancia con nombre), pon su puerto en `DB_PORT` o fija 1433 en la configuración TCP/IP.

Desde terminal también funciona: `DB_PASSWORD=tu_contraseña mvn spring-boot:run`.

**¿MySQL en lugar de SQL Server?** Añade `SPRING_PROFILES_ACTIVE=mysql` a las variables de entorno (usa `root` y crea la base de datos sola).

## Ver la landing con datos de la API

La landing debe abrirse desde un servidor local (por CORS, abrir `index.html` con doble clic no puede hablar con la API).

- **Desde IntelliJ:** abre `index.html` de la carpeta raíz y pulsa el icono del navegador que aparece arriba a la derecha (abre `http://localhost:63342/...`, que ya está permitido).
- **Desde terminal:** en la carpeta raíz, `python3 -m http.server 5500` y abre <http://localhost:5500>.

Si la API está apagada, la landing sigue funcionando con sus datos estáticos y el formulario muestra el teléfono en vez de fingir un envío.

## Endpoints

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | `/api/health` | 200 `{ "success": true, "message": "Backend funcionando correctamente." }` |
| GET | `/api/tratamientos` | 200, tratamientos activos ordenados |
| GET | `/api/tratamientos/{id}` | 200, o 404 si no existe o está inactivo |
| GET | `/api/odontologos` | 200, odontólogos activos ordenados |
| GET | `/api/odontologos/{id}` | 200, o 404 |
| POST | `/api/contacto` | 201, o 400 con errores por campo |
| POST | `/api/citas` | 201, o 400 con errores por campo |

Los campos JSON van en `snake_case`. Ejemplo de solicitud de cita:

```json
{
  "nombre": "Lucía Gómez",
  "telefono": "600 123 456",
  "email": "lucia@example.com",
  "tratamiento_id": 2,
  "fecha_preferida": "2026-10-20",
  "hora_preferida": "10:30",
  "mensaje": "Prefiero por la mañana"
}
```

Una solicitud de cita se guarda como `PENDIENTE`: **no confirma la cita**, la clínica contacta a la persona.

Error de validación (400):

```json
{
  "success": false,
  "message": "Revisa los datos enviados.",
  "errores": { "fecha_preferida": "La fecha preferida no puede ser anterior a hoy." }
}
```

Reglas: nombre (2–100, letras), teléfono (9–15 dígitos, admite `+`), email válido, mensaje hasta 1000 caracteres; en citas además tratamiento existente y activo, fecha no anterior a hoy (hora de Madrid) y hora opcional entre 07:00 y 20:00. Los errores inesperados devuelven 500 con un mensaje genérico, nunca la traza.

## Configuración (`src/main/resources/application.properties`)

- `server.port=8080`
- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`: conexión a SQL Server (por defecto `localhost:1433`, `sa`)
- Los textos se guardan como `nvarchar` (tildes, ñ y cualquier carácter)
- `app.cors.origenes`: orígenes permitidos (por defecto `localhost` y `127.0.0.1` en los puertos 5500 y 3000, más `localhost:63342` de IntelliJ; nunca `*`)
- `app.datos-iniciales`: carga los datos de prueba si las tablas están vacías
- `app.zona-horaria`: zona de la clínica para decidir qué es "hoy" (`Europe/Madrid`)

Las tablas se crean con `spring.jpa.hibernate.ddl-auto=update`, suficiente en desarrollo. Para producción conviene cambiarlo a `validate` y gestionar el esquema con scripts.

## Tests

```bash
mvn test
```

Usan una base de datos H2 en memoria, así que no necesitan SQL Server. En IntelliJ: clic derecho en `src/test/java` → *Run 'All Tests'*.

## Estructura

```text
src/main/java/sv/clinica/landing/
├── config/       CorsConfig, DataInitializer
├── controller/   Health, Tratamiento, Odontologo, Contacto, Cita
├── dto/          Respuestas, peticiones con validaciones y patrones compartidos
├── entity/       Tratamiento, Odontologo, SolicitudContacto, SolicitudCita y sus estados
├── repository/   Spring Data JPA
├── service/      Lógica de cada recurso
└── exception/    RecursoNoEncontradoException, DatosInvalidosException, GlobalExceptionHandler
```
