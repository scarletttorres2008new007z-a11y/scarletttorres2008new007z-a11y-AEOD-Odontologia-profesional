/**
 * Aparición sutil de bloques al entrar en pantalla (una sola vez).
 * No se activa con "reducir movimiento" ni sin IntersectionObserver,
 * y nunca oculta lo que ya está visible al cargar.
 */
(function () {
  'use strict';

  const AEOD = window.AEOD;
  if (!('IntersectionObserver' in window) || (AEOD && AEOD.prefersReducedMotion())) return;

  const SELECTORS = [
    '.section-head', '.service', '.finder', '.about__media', '.value', '.team-card',
    '.tech-card', '.plan', '.price-list', '.calc', '.faq__intro', '.contact-form',
  ];
  const targets = Array.from(document.querySelectorAll(SELECTORS.join(',')))
    .filter((el) => el.getBoundingClientRect().top > window.innerHeight);
  if (!targets.length) return;

  // Pequeño escalonado dentro de cada rejilla
  targets.forEach((el) => {
    const siblings = Array.from(el.parentElement.children).filter((c) => targets.includes(c));
    el.style.setProperty('--reveal-delay', `${Math.min(siblings.indexOf(el), 3) * 70}ms`);
    el.classList.add('reveal');
  });

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('is-visible');
      observer.unobserve(entry.target);
    });
  }, { rootMargin: '0px 0px -8% 0px' });

  targets.forEach((el) => observer.observe(el));
})();
