# Cómo abrir el sistema AEOD en tu equipo (XAMPP + IntelliJ)

El proyecto tiene ahora tres partes en carpetas separadas:

- `landing`: la web pública, la misma de siempre.
- `backend`: la API central. Es el antiguo backend de la landing, que ahora servirá también al software de gestión.
- `gestion`: el software de gestión para el personal (Fase 1: entrada con usuario y contraseña, usuarios, roles y
  auditoría). Se abre en VS Code (paso 8).

La base de datos central se llama **`clinica_aeod`**. **No tienes que crearla ni ejecutar ningún script SQL**: el backend
la crea solo la primera vez, con sus tablas y los datos de ejemplo.

Necesitas **XAMPP** e **IntelliJ IDEA** (Community o Ultimate), como hasta ahora, y el JDK **ms-21**. Para el software
de gestión, además, **VS Code** y **Node.js** (paso 8). MySQL Workbench es opcional (paso 9).

## ¿Ya tenías la versión de la Fase 0 (`aeod-sistema`)?

1. En IntelliJ, para el backend con el cuadrado rojo **■** y cierra IntelliJ.
2. Borra la carpeta `aeod-sistema` antigua y descomprime el zip nuevo en su lugar (paso 1).
3. Haz los pasos 3 a 8. En el paso 4 la clase ha cambiado de nombre: ahora es **`ClinicaApiApplication`**, así que crea
   la configuración otra vez desde ella.
4. Tu base `clinica_aeod` **se conserva**: el backend solo le añade las tablas nuevas (usuarios, roles, permisos,
   sesiones y auditoría). Tus citas de prueba siguen ahí.

## ¿Venías de la versión anterior (`aeod-landing-con-backend`)?

1. En IntelliJ, para el backend anterior con el cuadrado rojo **■** y cierra IntelliJ.
2. Haz los pasos 1 a 7 de esta guía con el zip nuevo. Es un proyecto nuevo, así que la configuración **AEOD XAMPP**
   no se usa: en el paso 4 creas una nueva y **ya no hace falta** `SPRING_PROFILES_ACTIVE=xampp`.
3. Tu base antigua **`clinica_landing` no se toca ni se usa**. Las reservas y mensajes de prueba que hiciste en ella no
   pasan a la nueva. Cuando todo funcione, puedes borrarla si quieres (paso 9) y borrar la carpeta
   `aeod-landing-con-backend`.

## 1. Descomprimir

1. Haz clic derecho en `aeod-sistema.zip` y elige **Extraer todo…**.
2. Se crea la carpeta `aeod-sistema`. Dentro verás `landing`, `backend`, `gestion` y esta guía.

## 2. Encender la base de datos (XAMPP)

1. Abre el **XAMPP Control Panel**.
2. Pulsa **Start** en la fila **MySQL**. El nombre se pone en verde y aparece el puerto **3306**.
3. Apache no hace falta. Solo enciéndelo si quieres usar phpMyAdmin.

## 3. Abrir el proyecto en IntelliJ

1. En IntelliJ ve a **File → Open…**.
2. Elige la carpeta **`aeod-sistema`** (la de arriba, no `backend`). Así IntelliJ abre también la landing.
3. Si pregunta si confías en el proyecto, pulsa **Trust Project**.
4. Abajo a la derecha aparece el aviso **"Maven build script found"**: pulsa **Load**.
   Si no aparece, en el panel izquierdo abre `backend`, haz clic derecho en `pom.xml` y elige **Add as Maven Project**.
5. Espera a que termine la barra de progreso de abajo. La primera vez descarga librerías y tarda unos minutos.
6. Ve a **File → Project Structure… → Project → SDK** y elige **ms-21**. Pulsa **OK**.

## 4. Crear la configuración de arranque

1. En el panel izquierdo abre `backend/src/main/java/sv/clinica/api/ClinicaApiApplication.java`.
2. Pulsa la flecha verde **▶** que aparece junto a `public class ClinicaApiApplication` y elige **Run**.
3. Ya está: **no hace falta ninguna variable de entorno**. Sin variables, el backend arranca en modo desarrollo y usa
   XAMPP (usuario `root` sin contraseña).

Si tu XAMPP tiene contraseña, entra en el desplegable de arriba a la derecha → **Edit Configurations…** y en
**Environment variables** escribe `DB_PASSWORD=tu_contraseña`.

## 5. Comprobar que el backend funciona

1. Abajo se abre la consola. La primera vez verás estas líneas:
   - `Successfully applied … migration(s) to schema clinica_aeod, now at version v2`: se han creado las tablas.
   - `Tomcat started on port 8080`: el backend está en marcha.
   - Varias líneas que empiezan por **`Datos de prueba:`**: tratamientos, odontólogos, horarios y almuerzo de ejemplo
     (solo si la base estaba vacía).
   - Un recuadro **«Administrador del software de gestión»** con el usuario `admin` y una **contraseña**.
     **Cópiala y guárdala**: solo se muestra esa vez y la necesitas en el paso 8.
2. Abre en el navegador <http://localhost:8080/api/health>. Debes ver:
   ```json
   {"success":true,"message":"Backend funcionando correctamente."}
   ```
3. Si quieres, comprueba también <http://localhost:8080/api/tratamientos>, que devuelve la lista de tratamientos.

Deja IntelliJ abierto con el backend en marcha. Para pararlo, pulsa el cuadrado rojo **■**.

## 6. Abrir la landing

1. En el panel izquierdo abre **`landing/index.html`**.
2. Pasa el ratón por la esquina superior derecha del editor. Aparecen iconos de navegadores: pulsa el de **Chrome**.
   Se abre una dirección como `http://localhost:63342/aeod-sistema/landing/index.html`, que el backend tiene permitida.
3. **Cómo saber que lee de la base de datos:** en la sección **Precios** aparece **"Valoración odontológica — Gratuita"**.
   Ese tratamiento solo existe en la base de datos, no en el HTML.

> No abras `index.html` con doble clic desde el explorador de Windows. La página se ve, pero el navegador no le deja
> hablar con el backend y usa los datos fijos del HTML.

## 7. Probar la reserva

1. Ve a **Contacto → Formulario de cita**.
2. En **1. Elige tu cita**, elige **Limpieza dental — 60 min** y un **día** entre semana.
3. Debajo aparecen los **Horarios disponibles** de ese día, con su odontólogo. Los calcula el backend. Nunca verás las
   12:00, porque es la hora del almuerzo.
4. Pulsa una hora. Junto al botón aparece **Tu horario** con el resumen.
5. En **2. Tus datos** escribe nombre, teléfono (p. ej. `600 123 456`) y correo, y pulsa **Confirmar cita**.
6. Debe salir *"Tu cita está reservada. Te llamaremos para confirmarla."* con el estado **Pendiente de confirmación**.
   Las citas de la web quedan pendientes hasta que la clínica las verifica.
7. Vuelve a elegir el mismo día: esa hora ya no aparece.

**Doble reserva:** abre la landing en dos pestañas, elige la misma hora en las dos y confirma primero en una y luego en
la otra. La segunda dice *"Este horario acaba de ser reservado. Selecciona otra opción."*.

## 8. Abrir el software de gestión (VS Code)

El software de gestión es una aplicación aparte para el personal. Habla solo con el backend (nunca con la base de
datos), así que el backend tiene que estar encendido (paso 5).

**Solo la primera vez:**

1. Instala **Node.js**: entra en <https://nodejs.org>, descarga la versión **LTS** (22 o superior) para Windows y
   ejecuta el instalador con las opciones que trae. Después, cierra y vuelve a abrir VS Code si estaba abierto.
2. En VS Code ve a **File → Open Folder…** y elige la carpeta **`aeod-sistema/gestion`**.
3. Abre la terminal con **Terminal → New Terminal** y escribe:
   ```
   npm install
   ```
   Tarda uno o dos minutos: descarga lo que necesita el software. Al acabar vuelve a aparecer el cursor.

**Cada vez que quieras usarlo:**

1. Con el backend encendido en IntelliJ, en la terminal de VS Code escribe:
   ```
   npm run dev
   ```
2. Cuando aparezca `Local: http://localhost:5173/`, abre esa dirección en el navegador.
3. Entra con el usuario **`admin`** y la contraseña del recuadro del paso 5.
4. Ve a **Mi cuenta** y cambia la contraseña por una tuya (mínimo 10 caracteres).
5. Para parar el software, en la terminal pulsa **Ctrl + C**.

**Para probar que los permisos funcionan:**

1. En **Usuarios → Nuevo usuario** crea, por ejemplo, a alguien de recepción y marca el rol **Recepción**.
2. Pulsa **Cerrar sesión** (arriba a la derecha) y entra con ese usuario: en el menú solo verá **Inicio**, porque en la
   Fase 1 el rol de recepción todavía no tiene permisos.
3. Si escribe a mano la dirección `http://localhost:5173/usuarios`, verá «No tienes permiso». Y aunque alguien llamara
   directamente a la API, el backend responde **403**: los permisos los comprueba siempre el backend.
4. En **Roles y permisos** (como administrador) decides qué puede hacer cada rol. Todo queda en **Auditoría**.

**Si pierdes la contraseña del administrador:**

1. En IntelliJ, desplegable de arriba a la derecha → **Edit Configurations…** → **Environment variables** y escribe
   `ADMIN_PASSWORD=UnaClaveNueva-2026` (la que quieras, mínimo 10 caracteres).
2. Arranca el backend y entra en el software con `admin` y esa contraseña.
3. **Quita la variable** de la configuración en cuanto entres, para que no se quede guardada ahí.

## 9. Ver la base de datos con MySQL Workbench (opcional)

**Instalar.** Descarga **MySQL Workbench** para Windows desde <https://dev.mysql.com/downloads/workbench/> (pulsa
**No thanks, just start my download**) e instálalo. Instala **solo Workbench, no MySQL Server**: la base de datos ya te
la da XAMPP, y dos servidores a la vez chocan en el puerto 3306.

**Conectar con XAMPP.**

1. Con MySQL encendido en XAMPP, abre Workbench y pulsa el **+** junto a **MySQL Connections**.
2. Rellena **Connection Name** `AEOD XAMPP`, **Hostname** `127.0.0.1`, **Port** `3306` y **Username** `root`.
3. Pulsa **Test Connection**. Si pide contraseña, déjala vacía y pulsa **OK**.
4. Saldrá un aviso **"Incompatible/nonstandard server version"**. Es normal: XAMPP usa MariaDB, que es compatible con MySQL.
   Pulsa **Continue Anyway** y después **OK**.
5. Haz doble clic en la conexión **AEOD XAMPP**.

**Ver los datos.** En el panel **Schemas** de la izquierda abre `clinica_aeod → Tables`. Clic derecho en `citas` →
**Select Rows - Limit 1000**: verás tu cita con el estado **PENDIENTE** y el origen **LANDING**. En `agenda_ocupacion`
hay una fila por cada cuarto de hora que ocupa.

**Qué no hacer.** No cambies la estructura de las tablas desde Workbench (ni columnas, ni tablas nuevas): esos cambios
los hacen las migraciones del backend. La tabla `flyway_schema_history` es el registro de esas migraciones; no la toques.
Editar datos, como horarios o bloqueos, sí está bien en tu equipo.

**Borrar la base antigua (opcional).** Cuando todo funcione, en el panel **Schemas** haz clic derecho en
`clinica_landing` → **Drop Schema…** → **Drop Now**. Comprueba bien el nombre: la que hay que conservar es `clinica_aeod`.

## 10. Cambiar horarios, turnos y bloqueos

Los horarios cargados son **de ejemplo**. Se cambian en la base de datos y el cambio se ve al momento en la landing:

| Tabla | Qué cambiar |
| --- | --- |
| `horarios_clinica` | Apertura y cierre de cada día. `dia_semana`: 1 = lunes … 7 = domingo. Si borras la fila de un día, ese día queda cerrado |
| `horarios_odontologo` | Turno de cada odontólogo cada día |
| `tratamientos` → `duracion_minutos` | Cuánto dura la cita de cada tratamiento |
| `odontologo_tratamientos` | Qué odontólogo hace cada tratamiento |
| `bloqueos` | Almuerzo, feriados, vacaciones, reuniones… |

Para añadir un **feriado**, en Workbench abre una pestaña SQL (icono **SQL+**), escribe esto cambiando la fecha y pulsa
el rayo **⚡**:

```sql
INSERT INTO clinica_aeod.bloqueos (tipo, motivo, fecha_inicio, fecha_fin, activo)
VALUES ('FERIADO', 'Fiesta Nacional', '2026-10-12', '2026-10-12', 1);
```

Más ejemplos en `backend/README.md`, apartado **Agenda**. Más adelante todo esto se hará desde el software de gestión.

## Si algo falla

| Lo que ves | Qué hacer |
| --- | --- |
| `Connection refused` o `Unable to obtain connection from database` en la consola | MySQL no está encendido en XAMPP. Pulsa **Start** y vuelve a ejecutar. |
| `Access denied for user 'root'` | Tu XAMPP tiene contraseña: añade `DB_PASSWORD=tu_contraseña` en las variables de entorno (paso 4). |
| `Faltan variables de entorno: DB_URL…` | Hay una variable `SPRING_PROFILES_ACTIVE` puesta en la configuración. En tu equipo quítala (o pon `dev`). |
| `Found non-empty schema(s) … but no schema history table` | El backend apunta a una base antigua. Quita la variable `DB_URL` de la configuración para que use `clinica_aeod`. |
| `Validate failed: Migrations have failed validation` | Una migración cambió después de aplicarse. En tu equipo, borra la base `clinica_aeod` (paso 9, igual que la antigua) y vuelve a arrancar: se crea de nuevo. Avísame si pasa. |
| `Port 8080 was already in use` | Ya hay otro backend en marcha (quizá el de la carpeta anterior). Páralo con el cuadrado rojo. |
| `release version 21 not supported` o errores de Java | El SDK no es Java 21: repite el punto 6 del paso 3 y elige **ms-21**. |
| No aparece la flecha verde ▶ | Maven no está cargado: clic derecho en `backend/pom.xml` → **Add as Maven Project**. |
| La landing no muestra "Valoración odontológica" | Comprueba que el backend está encendido (paso 5) y que abriste la página desde IntelliJ (paso 6), no con doble clic. |
| El formulario dice "No hemos podido enviar tu solicitud" | Igual que la fila anterior: el backend está apagado o la página no se abrió desde IntelliJ. |
| Línea roja `Duplicate entry … for key 'uk_agenda_ocupacion'` en la consola | Es normal: dos personas intentaron la misma hora y la base de datos rechazó la segunda. Es la protección contra la doble reserva. |
| El software dice «No se puede conectar con el servidor» | El backend está apagado. Arráncalo en IntelliJ (paso 4) y pulsa **Reintentar**. |
| `npm` no se reconoce como comando | Node.js no está instalado o VS Code se abrió antes de instalarlo. Instálalo (paso 8) y vuelve a abrir VS Code. |
| `Port 5173 is already in use` | El software ya está abierto en otra terminal. Usa esa, o ciérrala con **Ctrl + C**. |
| «Usuario o contraseña incorrectos» con `admin` | La contraseña es la del recuadro del paso 5 (distinta cada base). Si la perdiste, mira el final del paso 8. |
| «Demasiados intentos fallidos» | Tras 5 contraseñas mal seguidas, el usuario se bloquea 15 minutos. Espera, o que un administrador lo desbloquee en **Usuarios**. |
