/**
 * Carrusel de testimonios: pasa solo cada 7 segundos y, al llegar al final,
 * vuelve al principio. Se detiene mientras el ratón está encima, mientras se
 * toca en móvil, con el foco del teclado dentro, con la pestaña oculta o
 * fuera de pantalla, y con el botón de pausa. Con "reducir movimiento"
 * empieza en pausa (el botón permite activarlo).
 * Scroll-snap nativo: deslizar en móvil y flechas con el carrusel enfocado.
 */
(function () {
  'use strict';

  const INTERVAL = 7000;

  const viewport = document.querySelector('[data-carousel]');
  if (!viewport) return;

  const root = viewport.closest('.testimonials') || viewport.parentElement;
  const track = viewport.querySelector('[data-carousel-track]');
  const slides = Array.from(track.children);
  const controls = document.querySelector('[data-carousel-controls]');
  const prev = document.querySelector('[data-carousel-prev]');
  const next = document.querySelector('[data-carousel-next]');
  const toggle = document.querySelector('[data-carousel-toggle]');
  const status = document.querySelector('[data-carousel-status]');
  const progress = document.querySelector('[data-carousel-progress]');
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

  if (!slides.length || !controls) return;
  controls.hidden = false;

  const step = () => (slides.length < 2 ? viewport.clientWidth : slides[1].offsetLeft - slides[0].offsetLeft);
  const visibleCount = () => Math.max(1, Math.round(viewport.clientWidth / step()));
  const lastIndex = () => Math.max(0, slides.length - visibleCount());
  const clamp = (i) => Math.max(0, Math.min(i, lastIndex()));

  let index = clamp(Math.round(viewport.scrollLeft / step()));

  // ── Posición: "1–2 de 4"
  let lastText = '';
  function update() {
    const first = index + 1;
    const last = Math.min(index + visibleCount(), slides.length);
    const text = first === last ? `${first} de ${slides.length}` : `${first}–${last} de ${slides.length}`;
    if (text !== lastText) {
      status.textContent = text;
      lastText = text;
    }
  }

  // Con bucle: después del último vuelve al primero y viceversa.
  function goTo(target) {
    const positions = lastIndex() + 1;
    index = ((target % positions) + positions) % positions;
    viewport.scrollTo({ left: index * step(), behavior: reducedMotion.matches ? 'auto' : 'smooth' });
    update();
  }

  // ── Paso automático
  let autoplay = !reducedMotion.matches; // lo que decide el botón de pausa
  const holds = new Set();               // pausas temporales: ratón, foco, toque, pestaña, fuera de pantalla
  let timer = null;
  let remaining = INTERVAL;
  let startedAt = 0;

  const isRunning = () => autoplay && holds.size === 0 && lastIndex() > 0;

  function restartProgress() {
    if (!progress) return;
    progress.classList.remove('is-running');
    void progress.offsetWidth; // reinicia la animación CSS
    progress.classList.add('is-running');
  }

  function schedule() {
    startedAt = Date.now();
    timer = window.setTimeout(() => {
      timer = null;
      remaining = INTERVAL;
      goTo(index + 1);
      restartProgress();
      sync();
    }, remaining);
  }

  // Arranca o detiene el temporizador según el estado; al pausar guarda el tiempo restante.
  function sync() {
    const running = isRunning();
    if (running && !timer) {
      schedule();
    } else if (!running && timer) {
      window.clearTimeout(timer);
      timer = null;
      remaining = Math.max(0, remaining - (Date.now() - startedAt));
    }
    if (progress) progress.classList.toggle('is-paused', !running);
    // Mientras pasa solo no se anuncia cada cambio a los lectores de pantalla.
    status.setAttribute('aria-live', running ? 'off' : 'polite');
  }

  // Tras navegar a mano, el siguiente paso automático espera el intervalo completo.
  function resetTimer() {
    if (timer) window.clearTimeout(timer);
    timer = null;
    remaining = INTERVAL;
    restartProgress();
    sync();
  }

  function hold(reason, on) {
    if (on) holds.add(reason);
    else holds.delete(reason);
    sync();
  }

  function renderToggle() {
    if (!toggle) return;
    toggle.querySelector('use').setAttribute('href', autoplay ? '#i-pause' : '#i-play');
    toggle.querySelector('[data-carousel-toggle-label]').textContent = autoplay
      ? 'Pausar el paso automático de testimonios'
      : 'Reanudar el paso automático de testimonios';
  }

  if (toggle) {
    toggle.addEventListener('click', () => {
      autoplay = !autoplay;
      if (autoplay) holds.delete('focus'); // reanudado a propósito
      renderToggle();
      sync();
    });
  }

  prev.addEventListener('click', () => { goTo(index - 1); resetTimer(); });
  next.addEventListener('click', () => { goTo(index + 1); resetTimer(); });

  // Ratón encima (no cuenta el toque en móvil).
  viewport.addEventListener('pointerenter', (event) => { if (event.pointerType === 'mouse') hold('hover', true); });
  viewport.addEventListener('pointerleave', (event) => { if (event.pointerType === 'mouse') hold('hover', false); });

  // Deslizar con el dedo: pausa mientras se toca y reinicia el intervalo al soltar.
  viewport.addEventListener('touchstart', () => hold('touch', true), { passive: true });
  const endTouch = () => { if (holds.has('touch')) { holds.delete('touch'); resetTimer(); } };
  viewport.addEventListener('touchend', endTouch);
  viewport.addEventListener('touchcancel', endTouch);

  // Foco del teclado dentro de la sección (el botón de pausa no cuenta).
  root.addEventListener('focusin', (event) => {
    if (event.target === toggle) return;
    if (event.target.matches(':focus-visible')) hold('focus', true);
  });
  root.addEventListener('focusout', (event) => {
    if (!root.contains(event.relatedTarget)) hold('focus', false);
  });

  document.addEventListener('visibilitychange', () => hold('hidden', document.hidden));

  if ('IntersectionObserver' in window) {
    holds.add('offscreen');
    new IntersectionObserver((entries) => {
      const entry = entries[entries.length - 1];
      hold('offscreen', !(entry.isIntersecting && entry.intersectionRatio >= 0.35));
    }, { threshold: [0, 0.35, 0.7] }).observe(viewport);
  }

  const onMotionChange = () => {
    if (reducedMotion.matches && autoplay) {
      autoplay = false;
      renderToggle();
      sync();
    }
  };
  if (reducedMotion.addEventListener) reducedMotion.addEventListener('change', onMotionChange);
  else if (reducedMotion.addListener) reducedMotion.addListener(onMotionChange);

  // Deslizar o usar las flechas actualiza la posición al terminar el desplazamiento.
  let settle = null;
  viewport.addEventListener('scroll', () => {
    window.clearTimeout(settle);
    settle = window.setTimeout(() => {
      index = clamp(Math.round(viewport.scrollLeft / step()));
      update();
    }, 120);
  }, { passive: true });

  window.addEventListener('resize', () => {
    index = clamp(Math.round(viewport.scrollLeft / step()));
    update();
    sync();
  });

  root.style.setProperty('--carousel-interval', `${INTERVAL}ms`);
  if (progress) progress.hidden = false;
  if (document.hidden) holds.add('hidden');
  renderToggle();
  update();
  restartProgress();
  sync();
})();
