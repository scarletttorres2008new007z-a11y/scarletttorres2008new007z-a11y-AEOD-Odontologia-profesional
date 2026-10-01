/**
 * Estimador orientativo.
 * - Los precios se leen de la lista de precios del HTML (fuente única),
 *   que api-data.js actualiza con los datos de la API cuando está disponible.
 * - Se pueden sumar varios tratamientos ("Añadir a mi estimación").
 * - El seguro NO aplica ningún descuento: solo muestra una nota de cobertura.
 * - "Solicitar cita" lleva el resumen al mensaje del formulario.
 */
(function () {
  'use strict';

  const form = document.querySelector('[data-calculator]');
  const list = document.querySelector('[data-price-list]');
  const AEOD = window.AEOD;
  if (!form || !list || !AEOD) return;

  const { formatEuro, announce, session } = AEOD;
  const MAX_QTY = 32;
  const STORAGE_KEY = 'aeod-estimate';
  const $ = (sel) => form.querySelector(sel);
  const el = {
    treatment: $('#calc-treatment'),
    qty: $('#calc-qty'),
    qtyField: $('[data-calc-qty-field]'),
    months: $('#calc-months'),
    insurance: $('#calc-insurance'),
    empty: $('[data-calc-empty]'),
    filled: $('[data-calc-filled]'),
    price: $('[data-calc-price]'),
    monthly: $('[data-calc-monthly]'),
    insuranceNote: $('[data-calc-insurance]'),
    add: $('[data-calc-add]'),
    summary: $('[data-calc-summary]'),
    lines: $('[data-calc-lines]'),
    total: $('[data-calc-total]'),
    totalMonthly: $('[data-calc-total-monthly]'),
    cta: $('[data-calc-cta]'),
  };

  /* ── Catálogo desde el HTML (se reconstruye si la API actualiza los precios) ── */
  const catalog = new Map();
  function buildCatalog() {
    const selected = el.treatment.value;
    catalog.clear();
    el.treatment.length = 1; // conserva "Selecciona un tratamiento"
    list.querySelectorAll('.price-item').forEach((item) => {
      const id = item.dataset.id;
      const price = Number(item.dataset.price);
      if (!id || !Number.isFinite(price)) return;
      const entry = {
        id,
        name: item.querySelector('.price-item__name').textContent.trim(),
        price,
        perUnit: item.hasAttribute('data-per-unit'),
        formOption: item.dataset.form || '',
      };
      catalog.set(id, entry);
      el.treatment.add(new Option(entry.name + (entry.perUnit ? ' (por pieza)' : ''), id));
    });
    el.treatment.value = catalog.has(selected) ? selected : '';
  }
  buildCatalog();

  /* ── Estado: líneas añadidas [{ id, qty }] ── */
  let lines = (session.get(STORAGE_KEY) || []).filter((line) => catalog.has(line.id));

  const lineTotal = (line) => catalog.get(line.id).price * line.qty;
  const months = () => Number(el.months.value) || 1;

  function readQty() {
    const qty = Math.floor(Number(el.qty.value));
    if (!Number.isFinite(qty) || qty < 1) return 1;
    return Math.min(qty, MAX_QTY);
  }

  function monthlyText(total) {
    return months() > 1
      ? `Aproximadamente ${formatEuro(Math.ceil(total / months()))}/mes durante ${months()} meses, sin intereses. Financiación sujeta a aprobación.`
      : 'Pago único. También puedes financiarlo hasta 60 meses sin intereses, sujeto a aprobación.';
  }

  function lineLabel(line) {
    const entry = catalog.get(line.id);
    return entry.perUnit ? `${entry.name} × ${line.qty}` : entry.name;
  }

  /* ── Pintado ── */
  function renderCurrent() {
    const entry = catalog.get(el.treatment.value);
    el.qtyField.hidden = !entry || !entry.perUnit;
    el.insuranceNote.hidden = el.insurance.value !== 'si';
    el.add.disabled = !entry;

    if (!entry) {
      el.empty.hidden = false;
      el.filled.hidden = true;
      return;
    }
    const total = entry.price * (entry.perUnit ? readQty() : 1);
    el.price.textContent = formatEuro(total);
    el.monthly.textContent = monthlyText(total);
    // Con varias líneas, la financiación se muestra solo en el total
    el.monthly.hidden = lines.length > 0;
    el.empty.hidden = true;
    el.filled.hidden = false;
  }

  function renderSummary() {
    el.summary.hidden = lines.length === 0;
    el.lines.replaceChildren(...lines.map((line, index) => {
      const li = document.createElement('li');
      li.className = 'calc__line';
      const name = document.createElement('span');
      name.textContent = lineLabel(line);
      const price = document.createElement('span');
      price.className = 'calc__line-price';
      price.textContent = formatEuro(lineTotal(line));
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'calc__remove';
      remove.dataset.removeIndex = String(index);
      remove.setAttribute('aria-label', `Quitar ${lineLabel(line)}`);
      remove.textContent = 'Quitar';
      li.append(name, price, remove);
      return li;
    }));
    const total = lines.reduce((sum, line) => sum + lineTotal(line), 0);
    el.total.textContent = formatEuro(total);
    el.totalMonthly.textContent = monthlyText(total);
    session.set(STORAGE_KEY, lines);
    renderCurrent();
    updateCta();
  }

  /* El botón de cita lleva el tratamiento principal y un resumen al formulario */
  function updateCta() {
    const first = lines[0] || (catalog.has(el.treatment.value) ? { id: el.treatment.value, qty: readQty() } : null);
    el.cta.dataset.treatment = first ? catalog.get(first.id).formOption : '';
  }

  function summaryMessage() {
    const source = lines.length ? lines
      : catalog.has(el.treatment.value) ? [{ id: el.treatment.value, qty: catalog.get(el.treatment.value).perUnit ? readQty() : 1 }] : [];
    if (!source.length) return '';
    const total = source.reduce((sum, line) => sum + lineTotal(line), 0);
    return `Estimación orientativa desde la web: ${source.map(lineLabel).join(', ')} (desde ${formatEuro(total)}). Me gustaría pedir una valoración.`;
  }

  /* ── Eventos ── */
  form.addEventListener('input', () => { renderCurrent(); updateCta(); });
  form.addEventListener('change', (event) => {
    if (event.target === el.qty) el.qty.value = readQty();
    renderCurrent();
    if (event.target === el.months) renderSummary();
    updateCta();
  });

  el.add.addEventListener('click', () => {
    const entry = catalog.get(el.treatment.value);
    if (!entry) return;
    const qty = entry.perUnit ? readQty() : 1;
    const existing = lines.find((line) => line.id === entry.id);
    if (existing) existing.qty = entry.perUnit ? Math.min(existing.qty + qty, MAX_QTY) : 1;
    else lines.push({ id: entry.id, qty });
    renderSummary();
    const total = lines.reduce((sum, line) => sum + lineTotal(line), 0);
    announce(`${entry.name} añadido. Total orientativo desde ${formatEuro(total)}.`);
  });

  el.lines.addEventListener('click', (event) => {
    const button = event.target.closest('[data-remove-index]');
    if (!button) return;
    const [removed] = lines.splice(Number(button.dataset.removeIndex), 1);
    renderSummary();
    announce(`${catalog.get(removed.id).name} quitado de la estimación.`);
    const next = el.lines.querySelector('.calc__remove') || el.add;
    if (!next.disabled) next.focus(); else el.treatment.focus();
  });

  el.cta.addEventListener('click', () => {
    const message = summaryMessage();
    if (message) AEOD.prefillContact({ treatment: el.cta.dataset.treatment, message });
  });

  document.addEventListener('aeod:precios-actualizados', () => {
    buildCatalog();
    lines = lines.filter((line) => catalog.has(line.id));
    renderSummary();
  });

  renderCurrent();
  renderSummary();
})();
