/**
 * Modales de tratamiento con <dialog> nativo.
 * showModal() aporta role="dialog", modalidad, foco atrapado y Escape.
 * Aquí se añade: apertura por data-*, cierre al pulsar fuera,
 * bloqueo del scroll de fondo, devolución del foco y enlaces
 * compartibles (#tratamiento-ortodoncia abre su modal).
 */
(function () {
  'use strict';

  let opener = null;
  let urlBeforeOpen = null;

  const HASH_PREFIX = '#tratamiento-';
  const hashFor = (dialog) => HASH_PREFIX + dialog.id.replace(/^modal-/, '');
  const dialogForHash = (hash) => (hash.startsWith(HASH_PREFIX)
    ? document.getElementById('modal-' + hash.slice(HASH_PREFIX.length))
    : null);

  function open(dialog, trigger) {
    if (!dialog || dialog.open || typeof dialog.showModal !== 'function') return;
    opener = trigger || document.activeElement;
    dialog.showModal();
    document.body.classList.add('modal-open');
    // URL compartible sin desplazar la página
    urlBeforeOpen = window.location.href;
    window.history.replaceState(null, '', hashFor(dialog));
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
      if (window.location.hash === hashFor(dialog)) {
        window.history.replaceState(null, '', urlBeforeOpen && !urlBeforeOpen.includes(HASH_PREFIX)
          ? urlBeforeOpen
          : window.location.pathname + window.location.search);
      }
      urlBeforeOpen = null;
      const restore = dialog.dataset.restoreFocus !== 'false';
      delete dialog.dataset.restoreFocus;
      if (restore && opener && document.contains(opener)) opener.focus();
      opener = null;
    });
  });

  // Abrir el modal si se llega con un enlace compartido (#tratamiento-…)
  const openFromHash = () => {
    const dialog = dialogForHash(window.location.hash);
    if (dialog) open(dialog, document.querySelector(`[data-modal-open="${dialog.id}"]`));
  };
  window.addEventListener('hashchange', openFromHash);
  openFromHash();
})();
