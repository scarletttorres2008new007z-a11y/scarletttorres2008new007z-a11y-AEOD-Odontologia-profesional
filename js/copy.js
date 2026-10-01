/**
 * Botones "Copiar" junto al teléfono, email y dirección.
 * Solo aparecen si el navegador permite usar el portapapeles.
 */
(function () {
  'use strict';

  const buttons = document.querySelectorAll('[data-copy]');
  if (!buttons.length || !navigator.clipboard || !window.isSecureContext) return;

  buttons.forEach((button) => {
    const idle = button.textContent;
    const label = button.dataset.copyLabel || '';
    button.hidden = false;
    button.setAttribute('aria-label', `Copiar ${label}`.trim());

    let timer = null;
    button.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(button.dataset.copy);
        button.textContent = 'Copiado';
        button.classList.add('is-done');
        window.AEOD.announce(`${label ? label.charAt(0).toUpperCase() + label.slice(1) : 'Texto'} copiado`);
      } catch (error) {
        button.textContent = 'No se pudo copiar';
      }
      window.clearTimeout(timer);
      timer = window.setTimeout(() => {
        button.textContent = idle;
        button.classList.remove('is-done');
      }, 2000);
    });
  });
})();
