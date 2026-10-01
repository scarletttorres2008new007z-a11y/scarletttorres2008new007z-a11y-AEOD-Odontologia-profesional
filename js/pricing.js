/**
 * Filtro simple de la lista de precios orientativos por categoría.
 * Si la API actualiza la lista (api-data.js), se vuelve a aplicar el filtro activo.
 */
(function () {
  'use strict';

  const filters = document.querySelector('[data-price-filters]');
  const list = document.querySelector('[data-price-list]');
  const status = document.querySelector('[data-price-filter-status]');
  if (!filters || !list) return;

  const chips = Array.from(filters.querySelectorAll('[data-filter]'));
  filters.hidden = false;

  function apply(category) {
    let shown = 0;
    list.querySelectorAll('.price-item').forEach((item) => {
      const categories = (item.dataset.category || '').split(/\s+/);
      const visible = category === 'todos' || categories.includes(category);
      item.hidden = !visible;
      if (visible) shown += 1;
    });
    chips.forEach((chip) => {
      chip.setAttribute('aria-pressed', String(chip.dataset.filter === category));
    });
    if (status) {
      status.textContent = `Mostrando ${shown} ${shown === 1 ? 'tratamiento' : 'tratamientos'}`;
    }
  }

  filters.addEventListener('click', (event) => {
    const chip = event.target.closest('[data-filter]');
    if (chip) apply(chip.dataset.filter);
  });

  document.addEventListener('aeod:precios-actualizados', () => {
    const active = chips.find((chip) => chip.getAttribute('aria-pressed') === 'true');
    apply(active ? active.dataset.filter : 'todos');
  });
})();
