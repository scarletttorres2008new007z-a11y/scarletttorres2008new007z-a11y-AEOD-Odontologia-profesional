/**
 * Fotos sin conexión: las fotos vienen de Unsplash. Si alguna no carga
 * (sin internet, bloqueada o retirada), se sustituye por un fondo neutro
 * en lugar del icono de imagen rota.
 */
(function () {
  'use strict';

  // Rectángulo crema (#EEECE8); object-fit: cover lo ajusta a cada hueco.
  const BLANK = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2240%22 height=%2230%22%3E%3Crect width=%22100%25%22 height=%22100%25%22 fill=%22%23EEECE8%22/%3E%3C/svg%3E';

  function useFallback(img) {
    if (img.dataset.fallback) return;
    img.dataset.fallback = 'true';
    const picture = img.parentElement && img.parentElement.tagName === 'PICTURE' ? img.parentElement : null;
    if (picture) picture.querySelectorAll('source').forEach((source) => source.remove());
    img.removeAttribute('srcset');
    img.removeAttribute('sizes');
    img.src = BLANK;
    img.classList.add('is-unavailable');
  }

  // Errores que ocurran a partir de ahora (incluidas las imágenes diferidas)…
  document.addEventListener('error', (event) => {
    if (event.target instanceof HTMLImageElement) useFallback(event.target);
  }, true);

  // …y las que ya fallaron antes de que se ejecutara este script.
  document.querySelectorAll('img').forEach((img) => {
    if (img.complete && img.currentSrc && img.naturalWidth === 0) useFallback(img);
  });
})();
