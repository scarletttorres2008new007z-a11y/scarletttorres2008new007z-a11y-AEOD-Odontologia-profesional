# AEOD · Odontología Profesional — Frontend

Landing page de una sola página (HTML, CSS y JavaScript sin dependencias ni paso de compilación).

## Abrir en local

Basta con abrir `index.html` en el navegador. También funciona con cualquier servidor estático:

```bash
python3 -m http.server 8000
```

## Estructura

```text
index.html          Página completa (HTML semántico, sin estilos ni onclick en línea)
assets/             Favicon (y en el futuro las imágenes propias)
css/
  variables.css     Colores, tipografía, espaciado (tokens)
  base.css          Reset, tipografía, layout, utilidades, foco y reduced motion
  buttons.css       Botones y enlaces de acción
  forms.css         Campos de formulario (contacto y estimador)
  navbar.css        Cabecera sticky, menú móvil y barra inferior móvil
  hero.css · trust.css · services.css · about.css · team.css · technology.css
  testimonials.css · pricing.css · calculator.css · faq.css · contact.css
  footer.css · modal.css
js/
  navigation.js     Menú móvil, sección activa, foco en enlaces internos, barra móvil
  modal.js          Modales de tratamiento (<dialog> nativo)
  testimonials.js   Carrusel accesible
  pricing.js        Filtro de la lista de precios
  calculator.js     Estimador orientativo (lee los precios de la lista HTML)
  faq.js            Acordeón accesible
  api.js            Envío al backend (punto único de integración)
  contact-form.js   Validación, envío y estados del formulario
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

## Precios

Los precios orientativos se editan en un solo sitio: la lista `data-price-list` de la sección Precios en `index.html` (`data-price`, `data-per-unit`, `data-category`). El estimador los lee de ahí.

## Pendiente antes de producción

- Sustituir las imágenes de Unsplash por fotos propias en `assets/img/` (WebP/AVIF), manteniendo `width`/`height`, `srcset` y `sizes`.
- Crear `aviso-legal.html`, `privacidad.html` y `cookies.html`.
- Configurar `data-endpoint` del formulario.
- Añadir `og:url`, `og:image` y `link rel="canonical"` con el dominio definitivo.
- Verificar los datos de negocio (teléfonos, dirección, horario, cifras, credenciales y testimonios) antes de añadir datos estructurados de schema.org.
