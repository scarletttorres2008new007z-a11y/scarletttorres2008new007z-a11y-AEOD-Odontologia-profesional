# AEOD · Odontología Profesional — Frontend

Landing page de una sola página (HTML, CSS y JavaScript sin dependencias ni paso de compilación).
Todo se sirve en local (imágenes, fuentes y scripts): funciona sin conexión y abriendo el archivo directamente.

## Abrir en local

Basta con abrir `index.html` en el navegador. También funciona con cualquier servidor estático:

```bash
python3 -m http.server 8000
```

## Estructura

```text
index.html          Página completa (HTML semántico, sin estilos ni onclick en línea)
assets/
  img/              Ilustraciones WebP en varios anchos (para srcset)
  fonts/            Inter y Libre Baskerville (woff2, licencia OFL)
  favicon.svg
css/
  fonts.css         @font-face de las fuentes locales
  variables.css     Colores, tipografía, espaciado (tokens)
  base.css          Reset, tipografía, layout, utilidades, foco y reduced motion
  buttons.css       Botones y enlaces de acción
  forms.css         Campos de formulario (contacto y estimador)
  navbar.css        Cabecera sticky, menú móvil y barra inferior móvil
  hero.css · trust.css · services.css · about.css · team.css · technology.css
  testimonials.css · pricing.css · calculator.css · faq.css · contact.css
  footer.css · modal.css
js/
  core.js           Datos de la clínica (horario) y utilidades compartidas
  navigation.js     Menú móvil, sección activa, foco en enlaces internos, barra móvil, volver arriba
  modal.js          Modales de tratamiento (<dialog> nativo) con enlace compartible #tratamiento-…
  testimonials.js   Carrusel accesible
  pricing.js        Filtro de la lista de precios
  calculator.js     Estimador orientativo (lee los precios de la lista HTML)
  faq.js            Acordeón accesible
  api.js            Envío al backend (punto único de integración)
  contact-form.js   Validación, envío, estados, día/franja preferida, contador y borrador
  open-status.js    "Abierto ahora / Cerrado" con la hora de Madrid
  treatment-finder.js  Orientación rápida: 1–2 preguntas → tratamiento recomendado
  copy.js           Botones "Copiar" de teléfono, email y dirección
  reveal.js         Aparición sutil al hacer scroll (desactivada con reducir movimiento)
  main.js           Arranque general
```

Las media queries viven en el archivo de cada sección (mobile-first), por eso no hay un `responsive.css` aparte.

Los scripts son clásicos con `defer` (no `type="module"`) para que la página siga funcionando al abrir el archivo directamente (`file://`).

## Conectar el formulario a un backend

El formulario **no simula envíos**. Mientras no haya backend, muestra un error con el teléfono y el email.

Para activarlo, indica la URL de la API en `index.html`:

```html
<form ... data-contact-form data-endpoint="https://tu-api.com/citas">
```

Se envía un `POST` con JSON `{ name, phone, email, treatment, message }`. Cualquier respuesta 2xx se trata como éxito. La lógica está en `js/api.js`.

## Imágenes

Las imágenes actuales son ilustraciones de marca generadas para el proyecto (no fotos). Para poner fotos reales, guarda cada foto en `assets/img/` con el **mismo nombre y anchos** (por ejemplo `hero-640.webp`, `hero-960.webp`, `hero-1280.webp`, `hero-1600.webp`) y añade un `alt` descriptivo en `index.html`. Proporciones: hero 4:3, tratamientos 16:10, clínica 4:5, equipo 3:4.

## Horario

El horario de `js/core.js` alimenta el indicador "Abierto ahora" y la validación del día preferido. No contempla festivos.

## Precios

Los precios orientativos se editan en un solo sitio: la lista `data-price-list` de la sección Precios en `index.html` (`data-price`, `data-per-unit`, `data-category`). El estimador los lee de ahí.

## Pendiente antes de producción

- Sustituir las ilustraciones por fotos reales de la clínica y del equipo (ver «Imágenes»).
- Crear `aviso-legal.html`, `privacidad.html` y `cookies.html`.
- Configurar `data-endpoint` del formulario.
- Añadir `og:url`, `og:image` y `link rel="canonical"` con el dominio definitivo.
- Verificar los datos de negocio (teléfonos, dirección, horario, cifras, credenciales y testimonios) antes de añadir datos estructurados de schema.org.
