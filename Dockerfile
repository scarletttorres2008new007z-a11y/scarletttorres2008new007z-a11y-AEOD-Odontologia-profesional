# Servicio «web» de Railway: la landing en / y el software de gestión en /gestion/, en un mismo dominio.
# Todo lo que empieza por /api se pasa al backend por la red privada de Railway, así el navegador ve una
# sola web (sin CORS y con la cookie de sesión del software protegida). El backend tiene su propio Dockerfile.

# 1) Compila el software de gestión para servirlo desde /gestion/.
FROM node:22-alpine AS gestion
WORKDIR /app
COPY gestion/package.json gestion/package-lock.json ./
RUN npm ci
COPY gestion/ ./
RUN AEOD_BASE=/gestion/ npx vite build

# 2) Servidor web (Caddy) con la landing y el software ya compilado.
FROM caddy:2.10-alpine
COPY despliegue/Caddyfile /etc/caddy/Caddyfile
COPY landing/ /srv/landing/
COPY --from=gestion /app/dist/ /srv/gestion/
# La landing llama a la API en su mismo dominio (/api) en lugar de a localhost:8080.
RUN rm -f /srv/landing/README.md \
 && sed -i 's#<meta name="aeod-api" content="[^"]*">#<meta name="aeod-api" content="/api">#' /srv/landing/index.html \
 && grep -q '<meta name="aeod-api" content="/api">' /srv/landing/index.html
