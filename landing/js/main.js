/**
 * Arranque general. Cada módulo (navigation, modal, testimonials,
 * pricing, calculator, faq, api, contact-form) se inicializa solo
 * si encuentra su sección en la página.
 */
(function () {
  'use strict';

  document.querySelectorAll('[data-year]').forEach((node) => {
    node.textContent = String(new Date().getFullYear());
  });
})();
