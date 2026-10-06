# Cómo abrir el sistema AEOD en tu equipo (XAMPP + IntelliJ)

El proyecto tiene ahora tres partes en carpetas separadas:

- `landing`: la web pública, la misma de siempre.
- `backend`: la API central. Es el antiguo backend de la landing, que ahora servirá también al software de gestión.
- `gestion`: el software de gestión para el personal: entrada con usuario y contraseña, usuarios, roles, auditoría,
  **pacientes** (Fase 2), **agenda y citas** y la **configuración de la agenda**: odontólogos, tratamientos, horarios y
  bloqueos (Fase 3). Se abre en VS Code (paso 8).

La base de datos central se llama **`clinica_aeod`**. **No tienes que crearla ni ejecutar ningún script SQL**: el backend
la crea solo la primera vez, con sus tablas y los datos de ejemplo.

Necesitas **XAMPP** e **IntelliJ IDEA** (Community o Ultimate), como hasta ahora, y el JDK **ms-21**. Para el software
de gestión, además, **VS Code** y **Node.js** (paso 8). MySQL Workbench es opcional (paso 9).

## ¿Ya tenías una versión anterior de `aeod-sistema` (Fase 0, 1, 2 o 3)?

1. Para el backend en IntelliJ con el cuadrado rojo **■** y, si el software está abierto, pulsa **Ctrl + C** en la
   terminal de VS Code. Cierra IntelliJ y VS Code.
2. Borra la carpeta `aeod-sistema` antigua y descomprime el zip nuevo en su lugar (paso 1).
3. Haz los pasos 3 a 8, también lo de **Solo la primera vez** del paso 8 (`npm install`). En el paso 4 la clase es
   **`ClinicaApiApplication`**.
4. Tu base `clinica_aeod` **se conserva**: el backend solo le añade lo nuevo. En la consola verás
   `Migrating schema clinica_aeod to version "5 - configuracion de la agenda"` (si venías de la Fase 2, antes sale
   también la 4), y la contraseña de `admin` es la misma que ya tenías (esta vez no sale el recuadro). Tus citas,
   pacientes y usuarios de prueba siguen ahí.
5. En **VS Code** no hace falta repetir `npm install` si ya lo hiciste con la Fase 2 o la 3; si lo repites, no pasa
   nada.

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
   - `Successfully applied … migration(s) to schema clinica_aeod, now at version v4`: se han creado las tablas.
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
8. Debajo del mensaje de reserva está **Cancelar esta cita**. Si lo pulsas, te pregunta si estás segura; al confirmar,
   sale *"Tu cita se ha cancelado."* y esa hora vuelve a aparecer libre. La cita no se borra: queda como **Cancelada**.

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

**Para probar los pacientes y los permisos:**

1. En **Usuarios → Nuevo usuario** crea, por ejemplo, a alguien de recepción y marca el rol **Recepción**.
2. Pulsa **Cerrar sesión** (arriba a la derecha) y entra con ese usuario: en el menú verá **Inicio**, **Agenda**,
   **Pacientes** y **Bloqueos**, pero no Usuarios, Roles ni Auditoría.
3. En **Pacientes → Nuevo paciente** da de alta a alguien (un DNI válido de ejemplo es `12345678Z`). Después búscalo
   por nombre (sin tildes también vale), DNI, teléfono o código, ábrelo y edita sus datos desde su ficha.
4. Si escribe a mano la dirección `http://localhost:5173/usuarios`, verá «No tienes permiso». Y aunque alguien llamara
   directamente a la API, el backend responde **403**: los permisos los comprueba siempre el backend.
5. Vuelve a entrar como `admin`. En **Auditoría** verás el alta y cada cambio del paciente, con quién lo hizo. En
   **Roles y permisos** decides qué puede hacer cada rol: de entrada, Recepción y Coordinador dan de alta y editan
   pacientes, y Odontólogo solo los consulta.

**Para probar la agenda y las citas** (con `admin`, después de reservar en la landing como en el paso 7):

1. En **Inicio** verás **Por confirmar** con tu reserva de la web. Ábrela, o ve a **Agenda**: el día sale con una
   columna por odontólogo y la cita aparece en el color de «Por confirmar» y con **Sin ficha** (aún no está en la
   ficha de ningún paciente).
2. Pulsa la cita. Desde ahí puedes **Confirmar**, **Vincular a un paciente** (lo busca por el teléfono que dejó en la
   web, o lo das de alta con esos datos), **Mover a otro día u hora**, **Cancelar la cita** y escribir **notas
   internas**, que el paciente no ve. Una cita cancelada no se borra: queda en la agenda como Cancelada.
3. Pulsa **Nueva cita**: eliges paciente, tratamiento y día, y el backend te enseña los **huecos libres** de cada
   odontólogo. Al darla, esa hora **deja de salir en la landing**. Si la cancelas, **vuelve a salir**.
4. Si cancelas una cita desde la landing (paso 7, punto 8), en la agenda aparece como **Cancelada**.
5. **Semana** enseña los 7 días; pulsa el nombre de un día para verlo entero. Cada cita sale también en la ficha del
   paciente, en el apartado **Citas**, y cada cambio queda en **Auditoría**.
6. Un usuario con el rol **Odontólogo** solo ve la agenda del odontólogo vinculado a su usuario, y no puede dar ni mover
   citas. Su usuario se vincula en **Odontólogos** (paso 10).

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
**Select Rows - Limit 1000**: verás tu cita con el estado **PENDIENTE** y el origen **LANDING**, y las que des desde el
software con el origen **SOFTWARE** y su `paciente_id`. Es la misma tabla para las dos. En `agenda_ocupacion` hay una fila
por cada cuarto de hora que ocupa cada cita. En `pacientes` están los pacientes que des de alta en el software.

**Qué no hacer.** No cambies la estructura de las tablas desde Workbench (ni columnas, ni tablas nuevas): esos cambios
los hacen las migraciones del backend. La tabla `flyway_schema_history` es el registro de esas migraciones; no la toques.
Los horarios, turnos, tratamientos y bloqueos cámbialos desde el software (paso 10): el backend comprueba que los datos
son correctos y cada cambio queda en **Auditoría**.

**Borrar la base antigua (opcional).** Cuando todo funcione, en el panel **Schemas** haz clic derecho en
`clinica_landing` → **Drop Schema…** → **Drop Now**. Comprueba bien el nombre: la que hay que conservar es `clinica_aeod`.

## 10. Configurar la agenda desde el software

Los horarios, turnos y duraciones que trae el sistema son **de ejemplo**. Cámbialos por los reales desde el software de
gestión, con `admin`, en el apartado **Configuración** del menú. Cada cambio se nota **al momento** en la landing y en la
agenda, y queda en **Auditoría** con quién lo hizo.

1. **Horarios.** En «Horario de la clínica», **Cambiar**: marca los días que abre y pon a qué hora abre y cierra.
   Después, **Cambiar turnos** en cada odontólogo: un día puede tener varios tramos (mañana y tarde) y un día sin tramos
   no trabaja. Una cita solo se puede dar dentro del horario de la clínica **y** del turno de su odontólogo.
2. **Bloqueos.** El tiempo en que no se dan citas: festivos, vacaciones, reuniones, capacitaciones, mantenimiento o el
   almuerzo. Puede ser para toda la clínica o para un odontólogo; unos días concretos o un día de cada semana; todo el
   día o solo unas horas. Para quitarlo, **Quitar**.
3. **Tratamientos.** La **duración en la agenda** (de 15 en 15 minutos) es lo que ocupa cada cita. También cambias el
   precio «desde», quién lo hace y si se ofrece. Uno nuevo sale en la landing al momento; el que dejas de ofrecer
   desaparece de la web, pero no se borra.
4. **Odontólogos.** El alta, los datos que salen en la web (nombre, especialidad y presentación) y su **usuario del
   software**: con él vinculado, al entrar ve **solo su agenda**. Uno nuevo sale en la web sin foto, y tendrá huecos en
   cuanto le pongas sus turnos en **Horarios**.

**Si un cambio deja citas fuera.** Si cambias un horario o pones un bloqueo donde ya había citas pendientes o
confirmadas, el software te enseña **«N citas por revisar»** con la lista. **No se cancela ninguna sola**: ábrelas y
muévelas o cancélalas desde la agenda.

**Quién puede.** De entrada, Administrador y Coordinador cambian todo esto, y Recepción solo los **Bloqueos** (por
ejemplo, si un odontólogo no puede venir). Se cambia en **Roles y permisos**. Aunque alguien sin permiso llamara a la
API directamente, el backend responde **403**.

**Para probarlo:** pon un bloqueo **Festivo** para toda la clínica un día de la semana que viene. En la landing, elige
ese día en el formulario de cita: ya no ofrece horas y te propone las siguientes. Quita el bloqueo y vuelven a salir.

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
| Al dar o mover una cita: «Este horario acaba de ser reservado» | Alguien (otra persona o la web) cogió esa hora mientras elegías. La lista de huecos se actualiza sola: elige otro. |
| Un odontólogo ve «Tu usuario todavía no está vinculado a ningún odontólogo» | Su usuario aún no está unido a su ficha. Con `admin`, ve a **Odontólogos → Editar** y elígelo en **Usuario del software**. |
| Al guardar un horario o un bloqueo sale «N citas por revisar» | Son citas ya dadas que quedan fuera del horario nuevo o dentro del bloqueo. No se cancelan solas: ábrelas desde la lista y muévelas o cancélalas. |
| «Tiene N citas pendientes o confirmadas de hoy en adelante» al desactivar o dejar de ofrecer | Primero mueve o cancela esas citas en la **Agenda**, y vuelve a intentarlo. |
| Un odontólogo o un tratamiento nuevo no tiene huecos en la web | Un odontólogo nuevo necesita sus turnos en **Horarios**, y un tratamiento, alguien que lo haga en sus turnos. |
| «Demasiados intentos fallidos» | Tras 5 contraseñas mal seguidas, el usuario se bloquea 15 minutos. Espera, o que un administrador lo desbloquee en **Usuarios**. |
