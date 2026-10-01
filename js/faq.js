/**
 * FAQ: cada botón controla su respuesta (aria-expanded + aria-controls).
 * Se pueden abrir varias a la vez. Funciona con ratón, teclado y táctil
 * porque son <button> nativos.
 */
(function () {
  'use strict';

  const faq = document.querySelector('[data-faq]');
  if (!faq) return;

  faq.addEventListener('click', (event) => {
    const button = event.target.closest('button[aria-controls]');
    if (!button) return;
    const answer = document.getElementById(button.getAttribute('aria-controls'));
    if (!answer) return;
    const expanded = button.getAttribute('aria-expanded') === 'true';
    button.setAttribute('aria-expanded', String(!expanded));
    answer.hidden = expanded;
  });
})();
