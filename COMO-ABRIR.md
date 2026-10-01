# Cómo abrir la landing AEOD con su backend (XAMPP + IntelliJ)

Necesitas tener instalados **XAMPP** e **IntelliJ IDEA** (Community o Ultimate). Java 21 lo descarga IntelliJ en el paso 3.

## 1. Descomprimir

1. Haz clic derecho en `aeod-landing-con-backend.zip` y elige **Extraer todo…**.
2. Se crea la carpeta `aeod-landing-con-backend`. Dentro verás `index.html`, `css`, `js`, `assets` y `backend`.

## 2. Encender la base de datos (XAMPP)

1. Abre el **XAMPP Control Panel**.
2. Pulsa **Start** en la fila **MySQL**. El nombre se pone en verde y aparece el puerto **3306**.
3. Apache no hace falta. Solo enciéndelo si quieres usar phpMyAdmin en el paso 9.

## 3. Abrir el proyecto en IntelliJ

1. En IntelliJ ve a **File → Open…**.
2. Elige la carpeta **`aeod-landing-con-backend`**, la carpeta de arriba y no `backend`. Así IntelliJ puede abrir también la landing.
3. Si pregunta si confías en el proyecto, pulsa **Trust Project**.
4. Abajo a la derecha aparece el aviso **"Maven build script found"**: pulsa **Load**.
   Si no aparece, en el panel izquierdo abre `backend`, haz clic derecho en `pom.xml` y elige **Add as Maven Project**.
5. Espera a que termine la barra de progreso de abajo. La primera vez descarga librerías y tarda unos minutos.
6. Configura Java 21 en **File → Project Structure… → Project → SDK**.
   Si Java 21 no aparece en la lista, elige **Add SDK → Download JDK…**, selecciona la versión **21** y pulsa **Download**. Después pulsa **OK**.

## 4. Crear la base de datos desde IntelliJ

1. Abre el panel **Database**: el icono de la derecha con forma de cilindro, o **View → Tool Windows → Database**.
2. Pulsa **+ → Data Source → MariaDB**.
3. Rellena **Host** `localhost`, **Port** `3306` y **User** `root`, y deja **Password** vacío.
4. Si aparece **Download missing driver files**, pulsa el enlace. Después pulsa **Test Connection**: debe salir "Succeeded". Pulsa **OK**.
5. En el panel izquierdo abre `backend/database/crear-base-de-datos-mysql.sql`.
6. Pulsa la flecha verde **▶ Execute** que aparece arriba del archivo y, si pregunta, elige la conexión que acabas de crear.
7. Pulsa el botón de refrescar del panel Database: aparece el schema **clinica_landing**, todavía vacío.

> En IntelliJ Community no existe el panel Database. En ese caso enciende **Apache** en XAMPP, abre <http://localhost/phpmyadmin>, entra en la pestaña **SQL**, pega el contenido de ese archivo y pulsa **Continuar**.

## 5. Decirle al backend que use XAMPP

1. En el panel izquierdo abre `backend/src/main/java/sv/clinica/landing/ClinicaLandingApplication.java`.
2. Pulsa la flecha verde **▶** que aparece junto a `public class ClinicaLandingApplication` y elige **Run**. Si ese primer arranque falla, no pasa nada: sirve para que IntelliJ cree la configuración.
3. Arriba a la derecha, abre el desplegable con el nombre **ClinicaLandingApplication** y elige **Edit Configurations…**.
4. En **Environment variables** escribe:
   ```
   SPRING_PROFILES_ACTIVE=xampp
   ```
   Si no ves ese campo, pulsa **Modify options → Environment variables**.
5. Pulsa **OK**.

## 6. Arrancar el backend

1. Pulsa el botón verde **▶ Run**, arriba a la derecha.
2. Abajo se abre la consola. Cuando termine verás una línea con **`Tomcat started on port 8080`** y otra con **`Datos de prueba: tratamientos insertados`**. Esta segunda solo sale la primera vez.
3. Abre en el navegador <http://localhost:8080/api/health>. Debes ver:
   ```json
   {"success":true,"message":"Backend funcionando correctamente."}
   ```
4. Si quieres, comprueba también <http://localhost:8080/api/tratamientos>, que devuelve la lista de tratamientos.

Deja IntelliJ abierto con el backend en marcha. Para pararlo, pulsa el cuadrado rojo **■**.

## 7. Abrir la landing

1. En el panel izquierdo de IntelliJ abre **`index.html`**, el de la carpeta de arriba.
2. Pasa el ratón por la esquina superior derecha del editor. Aparecen iconos de navegadores: pulsa el de **Chrome**.
   Se abre una dirección como `http://localhost:63342/aeod-landing-con-backend/index.html`, que el backend tiene permitida.
3. **Cómo saber que lee de la base de datos:** ve a la sección **Precios**. En la lista aparece **"Valoración odontológica — Gratuita"**. Ese tratamiento solo existe en la base de datos, no en el HTML.

> No abras `index.html` con doble clic desde el explorador de Windows. La página se ve, pero el navegador no le deja hablar con el backend y usa los datos fijos del HTML.

## 8. Probar el formulario

1. Ve a **Contacto → Formulario de cita**.
2. Escribe nombre, teléfono (p. ej. `600 123 456`) y correo, elige **Limpieza dental** y un **día preferido** (de lunes a sábado).
3. Pulsa **Solicitar cita**. Debe salir: *"Hemos recibido tu solicitud de cita. Aún no está confirmada…"*.
4. Si lo envías **sin día**, se guarda como solicitud de contacto.

## 9. Ver que se guardó en la base de datos

- **En IntelliJ:** en el panel **Database**, refresca y abre `clinica_landing → tables → solicitudes_cita` con doble clic. Verás tu solicitud con el estado **PENDIENTE**. Las enviadas sin día están en `solicitudes_contacto`, con el estado **NUEVO**.
- **En phpMyAdmin:** con Apache encendido, entra en <http://localhost/phpmyadmin> → `clinica_landing` → `solicitudes_cita`.

## Si algo falla

| Lo que ves | Qué hacer |
| --- | --- |
| `Communications link failure` o `Connection refused` en la consola | MySQL no está encendido en XAMPP. Pulsa **Start** y vuelve a ejecutar. |
| `Unknown database 'clinica_landing'` | Falta el paso 4: ejecuta el script `crear-base-de-datos-mysql.sql`. |
| `Access denied for user 'root'` | Tu XAMPP tiene contraseña. Añade `;DB_PASSWORD=tu_contraseña` a las variables de entorno del paso 5. |
| Habla de `sqlserver` o `1433` | Falta la variable `SPRING_PROFILES_ACTIVE=xampp` (paso 5). |
| `Port 8080 was already in use` | Ya hay otro backend en marcha. Páralo con el cuadrado rojo, o cierra el programa que use el puerto 8080. |
| `release version 21 not supported` o errores de Java | El SDK no es Java 21: repite el punto 6 del paso 3. |
| No aparece la flecha verde ▶ | Maven no está cargado: haz clic derecho en `backend/pom.xml` → **Add as Maven Project**. |
| La landing no muestra "Valoración odontológica" | Comprueba que el backend está encendido (paso 6.3) y que abriste la página desde IntelliJ (paso 7), no con doble clic. |
| El formulario dice "No hemos podido enviar tu solicitud" | Igual que la fila anterior: el backend está apagado o la página no se abrió desde IntelliJ. |

**¿Con SQL Server en vez de XAMPP?** Ejecuta `backend/database/crear-base-de-datos.sql` en SSMS. En el paso 5 pon `DB_USER=sa;DB_PASSWORD=tu_contraseña` en lugar de `SPRING_PROFILES_ACTIVE=xampp`. El resto es igual.
