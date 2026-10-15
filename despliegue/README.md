# Despliegue en Railway

Un proyecto de Railway con tres servicios, todos desde este repositorio:

| Servicio  | Qué es                                   | Cómo se construye                                  | Dominio público |
|-----------|------------------------------------------|----------------------------------------------------|-----------------|
| `MySQL`   | Base de datos (plantilla de Railway)     | —                                                  | No              |
| `backend` | API Spring Boot (perfil `prod`)          | `backend/Dockerfile` (Root Directory = `/backend`) | No              |
| `web`     | Landing en `/` y gestión en `/gestion/`  | `Dockerfile` de la raíz (Caddy)                    | Sí              |

`web` pasa `/api/*` a `http://backend.railway.internal:8080` por la red privada, así el navegador ve una sola web:
sin CORS y con la cookie de sesión (`SameSite=Strict`, `Path=/api/auth`) en el mismo sitio que el software.
Si el servicio del backend no se llama `backend`, define en `web` la variable
`API_INTERNA=http://${{<nombre>.RAILWAY_PRIVATE_DOMAIN}}:8080`.

## Variables del servicio `backend` (Raw Editor)

```
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
CORS_ALLOWED_ORIGINS=https://<dominio-del-servicio-web>
JWT_SECRET=<40 o más caracteres aleatorios>
ADMIN_USERNAME=admin
ADMIN_EMAIL=<correo del administrador>
ADMIN_PASSWORD=<contraseña inicial; quitar la variable tras el primer arranque>
```

`SPRING_PROFILES_ACTIVE=prod` ya lo fija el Dockerfile. En producción no hay datos de ejemplo: tratamientos,
odontólogos y horarios se dan de alta en Gestión → Configuración.

`useSSL=false` es correcto porque la conexión va por la red privada de Railway (túnel cifrado).

## Comprobado localmente

Imágenes con MySQL 8.4 y 9.7, red con alias `backend.railway.internal`: Flyway aplica V1–V5, `/api/health` responde
a través de `web`, la landing usa `/api`, y en `/gestion/` funcionan el login, la renovación de sesión al recargar
y los enlaces directos (`/gestion/pacientes`). La IP del visitante (`X-Real-IP` del proxy de Railway) llega al backend.
