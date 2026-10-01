/**
 * Modales de tratamiento con <dialog> nativo.
 * showModal() aporta role="dialog", modalidad, foco atrapado y Escape.
 * Aquí se añade: apertura por data-*, cierre al pulsar fuera,
 * bloqueo del scroll de fondo y devolución del foco.
 */
(function () {
  'use strict';

  let opener = null;

  function open(dialog, trigger) {
    if (!dialog || typeof dialog.showModal !== 'function') return;
    opener = trigger || document.activeElement;
    dialog.showModal();
    document.body.classList.add('modal-open');
  }

  function close(dialog, { restoreFocus = true } = {}) {
    if (!dialog || !dialog.open) return;
    dialog.dataset.restoreFocus = String(restoreFocus);
    dialog.close();
    document.body.classList.remove('modal-open');
  }

  document.addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-modal-open]');
    if (trigger) {
      open(document.getElementById(trigger.dataset.modalOpen), trigger);
      return;
    }

    const closer = event.target.closest('[data-modal-close]');
    if (closer) {
      // Si el botón lleva a otra sección (p. ej. #contacto), el foco va allí y no al botón de origen.
      const isInPageLink = closer.matches('a[href^="#"]');
      close(closer.closest('dialog'), { restoreFocus: !isInPageLink });
    }
  });

  document.querySelectorAll('dialog.modal').forEach((dialog) => {
    // Clic en el fondo (fuera de la caja del diálogo)
    dialog.addEventListener('click', (event) => {
      if (event.target !== dialog) return;
      const rect = dialog.getBoundingClientRect();
      const inside = event.clientX >= rect.left && event.clientX <= rect.right &&
                     event.clientY >= rect.top && event.clientY <= rect.bottom;
      if (!inside) close(dialog);
    });

    // Cierre por cualquier vía (Escape incluido)
    dialog.addEventListener('close', () => {
      document.body.classList.remove('modal-open');
      const restore = dialog.dataset.restoreFocus !== 'false';
      delete dialog.dataset.restoreFocus;
      if (restore && opener && document.contains(opener)) opener.focus();
      opener = null;
    });
  });
})();
