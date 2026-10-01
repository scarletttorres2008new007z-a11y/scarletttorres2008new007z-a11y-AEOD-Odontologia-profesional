/**
 * Carrusel de testimonios: scroll-snap nativo (deslizar en móvil,
 * flechas del teclado con el carrusel enfocado) + botones anterior/siguiente.
 * Sin reproducción automática.
 */
(function () {
  'use strict';

  const viewport = document.querySelector('[data-carousel]');
  if (!viewport) return;

  const track = viewport.querySelector('[data-carousel-track]');
  const slides = Array.from(track.children);
  const controls = document.querySelector('[data-carousel-controls]');
  const prev = document.querySelector('[data-carousel-prev]');
  const next = document.querySelector('[data-carousel-next]');
  const status = document.querySelector('[data-carousel-status]');
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

  if (!slides.length || !controls) return;
  controls.hidden = false;

  const step = () => {
    if (slides.length < 2) return viewport.clientWidth;
    return slides[1].offsetLeft - slides[0].offsetLeft;
  };
  const visibleCount = () => Math.max(1, Math.round(viewport.clientWidth / step()));
  const currentIndex = () => Math.round(viewport.scrollLeft / step());
  const lastIndex = () => Math.max(0, slides.length - visibleCount());

  let lastText = '';
  function update() {
    const index = Math.min(currentIndex(), lastIndex());
    const visible = visibleCount();
    const first = index + 1;
    const last = Math.min(index + visible, slides.length);
    const text = first === last
      ? `${first} de ${slides.length}`
      : `${first}–${last} de ${slides.length}`;
    if (text !== lastText) {
      status.textContent = text;
      lastText = text;
    }
    prev.disabled = index <= 0;
    next.disabled = index >= lastIndex();
  }

  function goTo(index) {
    const target = Math.max(0, Math.min(index, lastIndex()));
    viewport.scrollTo({
      left: target * step(),
      behavior: reducedMotion.matches ? 'auto' : 'smooth',
    });
  }

  prev.addEventListener('click', () => goTo(currentIndex() - 1));
  next.addEventListener('click', () => goTo(currentIndex() + 1));

  let ticking = false;
  viewport.addEventListener('scroll', () => {
    if (ticking) return;
    ticking = true;
    window.requestAnimationFrame(() => { update(); ticking = false; });
  }, { passive: true });
  window.addEventListener('resize', update);

  update();
})();
