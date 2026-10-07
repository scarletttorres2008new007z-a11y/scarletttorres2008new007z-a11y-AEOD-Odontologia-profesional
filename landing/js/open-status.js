/**
 * Estado de apertura en tiempo real ("Abierto ahora · hasta las 21:00").
 * Usa la hora de Madrid, sea cual sea la zona horaria del visitante.
 */
(function () {
  'use strict';

  const nodes = document.querySelectorAll('[data-open-status]');
  const { clinic, clinicNow, formatTime } = window.AEOD || {};
  if (!nodes.length || !clinic) return;

  const DAY_NAMES = ['el domingo', 'el lunes', 'el martes', 'el miércoles', 'el jueves', 'el viernes', 'el sábado'];

  function nextOpening(now) {
    for (let offset = 0; offset < 7; offset += 1) {
      const day = (now.day + offset) % 7;
      const hours = clinic.schedule[day];
      if (!hours) continue;
      if (offset === 0 && now.minutes >= hours.open) continue;
      const when = offset === 0 ? 'hoy' : offset === 1 ? 'mañana' : DAY_NAMES[day];
      return `${when} a las ${formatTime(hours.open)}`;
    }
    return '';
  }

  function render() {
    const now = clinicNow();
    const today = clinic.schedule[now.day];
    const isOpen = !!today && now.minutes >= today.open && now.minutes < today.close;
    const text = isOpen
      ? `Abierto ahora · hasta las ${formatTime(today.close)}`
      : `Cerrado ahora · abrimos ${nextOpening(now)}`;

    nodes.forEach((node) => {
      // La versión "compact" omite las urgencias cuando ya aparecen al lado
      const compact = node.dataset.openStatus === 'compact';
      node.hidden = false;
      node.classList.toggle('is-open', isOpen);
      node.textContent = isOpen || compact ? text : `${text} · Urgencias 24 h`;
    });
  }

  render();
  window.setInterval(render, 60 * 1000);
})();
