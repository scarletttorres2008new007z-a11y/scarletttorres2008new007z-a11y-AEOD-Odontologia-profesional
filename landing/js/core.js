/**
 * Núcleo compartido: datos de la clínica y utilidades comunes.
 * Se carga antes que el resto de módulos y expone window.AEOD.
 */
(function () {
  'use strict';

  /* Horario (hora de Madrid). 0 = domingo … 6 = sábado. No contempla festivos. */
  const clinic = {
    timeZone: 'Europe/Madrid',
    phone: '900 00 00 00',
    phoneHref: 'tel:+34900000000',
    schedule: {
      0: null,
      1: { open: 9 * 60, close: 21 * 60 },
      2: { open: 9 * 60, close: 21 * 60 },
      3: { open: 9 * 60, close: 21 * 60 },
      4: { open: 9 * 60, close: 21 * 60 },
      5: { open: 9 * 60, close: 21 * 60 },
      6: { open: 10 * 60, close: 14 * 60 },
    },
  };

  const WEEKDAYS = { Sun: 0, Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6 };
  const madridParts = new Intl.DateTimeFormat('en-US', {
    timeZone: clinic.timeZone,
    weekday: 'short', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  });

  /** Fecha y hora actuales en Madrid: { day, minutes, isoDate } */
  function clinicNow(date = new Date()) {
    const parts = Object.fromEntries(madridParts.formatToParts(date).map((p) => [p.type, p.value]));
    return {
      day: WEEKDAYS[parts.weekday],
      minutes: Number(parts.hour) * 60 + Number(parts.minute),
      isoDate: `${parts.year}-${parts.month}-${parts.day}`,
    };
  }

  /** 1800 → "1.800 €" (agrupa también los miles de 4 cifras) */
  function formatEuro(value) {
    return String(Math.round(value)).replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ' €';
  }

  /** 540 → "09:00" */
  function formatTime(minutes) {
    const h = String(Math.floor(minutes / 60)).padStart(2, '0');
    const m = String(minutes % 60).padStart(2, '0');
    return `${h}:${m}`;
  }

  /** Mensaje para lectores de pantalla en la región aria-live global */
  function announce(message) {
    const region = document.querySelector('[data-announcer]');
    if (!region) return;
    region.textContent = '';
    window.setTimeout(() => { region.textContent = message; }, 50);
  }

  const prefersReducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /** Almacenamiento de sesión tolerante a errores (modo privado, bloqueos…) */
  const session = {
    get(key) {
      try { return JSON.parse(window.sessionStorage.getItem(key)); } catch (e) { return null; }
    },
    set(key, value) {
      try { window.sessionStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* sin almacenamiento */ }
    },
    remove(key) {
      try { window.sessionStorage.removeItem(key); } catch (e) { /* sin almacenamiento */ }
    },
  };

  /** Rellena el formulario de cita desde otros módulos (estimador, orientación). */
  function prefillContact(detail) {
    document.dispatchEvent(new CustomEvent('aeod:prefill-contact', { detail }));
  }

  window.AEOD = Object.assign(window.AEOD || {}, {
    clinic, clinicNow, formatEuro, formatTime, announce, prefersReducedMotion, session, prefillContact,
  });
})();
