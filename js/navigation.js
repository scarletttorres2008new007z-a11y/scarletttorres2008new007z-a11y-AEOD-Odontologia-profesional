/**
 * Navegación: cabecera sticky, menú móvil, sección activa,
 * foco en enlaces internos y barra de acción rápida en móvil.
 */
(function () {
  'use strict';

  const header = document.querySelector('[data-header]');
  const nav = document.querySelector('[data-nav]');
  const toggle = document.querySelector('[data-nav-toggle]');
  const toggleLabel = document.querySelector('[data-nav-toggle-label]');
  const desktopQuery = window.matchMedia('(min-width: 1024px)');

  /* ── Sombra de la cabecera al hacer scroll ── */
  if (header) {
    const onScroll = () => header.classList.toggle('is-scrolled', window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  /* ── Menú móvil ── */
  function setMenu(open, { returnFocus = false } = {}) {
    if (!nav || !toggle) return;
    nav.classList.toggle('is-open', open);
    document.body.classList.toggle('nav-open', open);
    toggle.setAttribute('aria-expanded', String(open));
    if (toggleLabel) toggleLabel.textContent = open ? 'Cerrar menú' : 'Abrir menú';
    if (open) {
      const firstLink = nav.querySelector('a');
      if (firstLink) firstLink.focus();
    } else if (returnFocus) {
      toggle.focus();
    }
  }

  if (nav && toggle) {
    toggle.addEventListener('click', () => {
      setMenu(toggle.getAttribute('aria-expanded') !== 'true');
    });

    nav.addEventListener('click', (event) => {
      if (event.target.closest('a')) setMenu(false);
    });

    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && nav.classList.contains('is-open')) {
        setMenu(false, { returnFocus: true });
      }
    });

    desktopQuery.addEventListener('change', (event) => {
      if (event.matches) setMenu(false);
    });
  }

  /* ── Enlace activo según la sección visible ── */
  const links = nav ? Array.from(nav.querySelectorAll('.nav__link[href^="#"]')) : [];
  const sections = links
    .map((link) => document.querySelector(link.getAttribute('href')))
    .filter(Boolean);

  function setActive(id) {
    links.forEach((link) => {
      const active = link.getAttribute('href') === '#' + id;
      link.classList.toggle('is-active', active);
      if (active) link.setAttribute('aria-current', 'true');
      else link.removeAttribute('aria-current');
    });
  }

  if ('IntersectionObserver' in window && sections.length) {
    const spy = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) setActive(entry.target.id);
      });
    }, { rootMargin: '-45% 0px -50% 0px' });
    sections.forEach((section) => spy.observe(section));
  }

  /* ── Enlaces internos: mover el foco al destino (teclado y lectores) ── */
  document.addEventListener('click', (event) => {
    const link = event.target.closest('a[href^="#"]');
    if (!link) return;
    const id = link.getAttribute('href').slice(1);
    const target = id && document.getElementById(id);
    if (!target) return;
    // Se espera al desplazamiento nativo y luego se enfoca sin volver a desplazar.
    window.setTimeout(() => {
      if (!target.hasAttribute('tabindex')) target.setAttribute('tabindex', '-1');
      target.focus({ preventScroll: true });
    }, 0);
  });

  /* ── Barra móvil: se oculta cuando el formulario de contacto está a la vista ── */
  const mobileCta = document.querySelector('[data-mobile-cta]');
  const contact = document.getElementById('contacto');
  if (mobileCta && contact && 'IntersectionObserver' in window) {
    const observer = new IntersectionObserver(([entry]) => {
      mobileCta.classList.toggle('is-hidden', entry.isIntersecting);
    }, { threshold: 0.15 });
    observer.observe(contact);
  }

  /* ── Botón "Volver arriba" ── */
  const toTop = document.querySelector('[data-to-top]');
  if (toTop) {
    toTop.hidden = false;
    const onScrollTop = () => toTop.classList.toggle('is-visible', window.scrollY > window.innerHeight * 1.5);
    window.addEventListener('scroll', onScrollTop, { passive: true });
    onScrollTop();
    toTop.addEventListener('click', () => {
      const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      window.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' });
      const logo = document.querySelector('.site-header .logo');
      if (logo) logo.focus({ preventScroll: true });
    });
  }
})();
