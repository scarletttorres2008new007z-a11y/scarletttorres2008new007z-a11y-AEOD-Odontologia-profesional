# AEOD · Odontología Profesional — Frontend

Landing page de una sola página (HTML, CSS y JavaScript sin dependencias ni paso de compilación).
Fuentes y scripts van en local y la página funciona abriendo el archivo directamente. Las fotos se cargan desde Unsplash, así que necesitan conexión a internet (sin conexión se ve un fondo neutro en su lugar).

## Abrir en local

Basta con abrir `index.html` en el navegador. También funciona con cualquier servidor estático:

```bash
python3 -m http.server 8000
```

## Estructura

```text
index.html          Página completa (HTML semántico, sin estilos ni onclick en línea)
assets/
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
  image-fallback.js Fondo neutro si una foto no carga (sin conexión)
  testimonials.js   Carrusel que pasa solo cada 7 s, con pausa (botón, ratón, foco y toque)
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

Las fotos son las de Unsplash del archivo original, servidas con `srcset` en varios anchos y recortadas por Unsplash a la proporción de cada hueco (hero 4:3, tratamientos 16:10, clínica 4:3 en móvil y 4:5 en escritorio, equipo 3:4 centrado en la cara). Cambios respecto al original:

- La foto de la clínica tenía el enlace mal escrito (`…daab30f310e5`, error 404). El correcto es `…daab30f310ce`.
- La foto del Dr. Marcos Ortega ya no existía (404). Ahora usa la foto masculina que en el original estaba asignada a la Dra. Rocío Fernández.
- Las fotos de la Dra. Rocío Fernández y la Dra. Ana Villar eran de hombres. Ahora son fotos de doctoras de Unsplash.

Son fotos de stock: no son los profesionales reales. Para usar fotos propias, guárdalas en una carpeta (por ejemplo `assets/img/`) y cambia `src` y `srcset` en `index.html` manteniendo las proporciones.

## Horario

El horario de `js/core.js` alimenta el indicador "Abierto ahora" y la validación del día preferido. No contempla festivos.

## Precios

Los precios orientativos se editan en un solo sitio: la lista `data-price-list` de la sección Precios en `index.html` (`data-price`, `data-per-unit`, `data-category`). El estimador los lee de ahí.

## Pendiente antes de producción

- Sustituir las fotos de stock por fotos propias de la clínica y del equipo (ver «Imágenes»).
- Crear `aviso-legal.html`, `privacidad.html` y `cookies.html`.
- Configurar `data-endpoint` del formulario.
- Añadir `og:url`, `og:image` y `link rel="canonical"` con el dominio definitivo.
- Verificar los datos de negocio (teléfonos, dirección, horario, cifras, credenciales y testimonios) antes de añadir datos estructurados de schema.org.
