/**
 * Estimador orientativo.
 * Los precios se leen de la lista de precios del HTML (fuente única).
 * El seguro NO aplica ningún descuento: solo muestra una nota de cobertura.
 */
(function () {
  'use strict';

  const form = document.querySelector('[data-calculator]');
  const list = document.querySelector('[data-price-list]');
  if (!form || !list) return;

  const MAX_QTY = 32;
  const el = {
    treatment: form.querySelector('#calc-treatment'),
    qty: form.querySelector('#calc-qty'),
    qtyField: form.querySelector('[data-calc-qty-field]'),
    months: form.querySelector('#calc-months'),
    insurance: form.querySelector('#calc-insurance'),
    empty: form.querySelector('[data-calc-empty]'),
    filled: form.querySelector('[data-calc-filled]'),
    price: form.querySelector('[data-calc-price]'),
    monthly: form.querySelector('[data-calc-monthly]'),
    insuranceNote: form.querySelector('[data-calc-insurance]'),
    cta: form.querySelector('[data-calc-cta]'),
  };

  // Catálogo construido desde el HTML
  const catalog = new Map();
  list.querySelectorAll('.price-item').forEach((item) => {
    const id = item.dataset.id;
    const price = Number(item.dataset.price);
    if (!id || !Number.isFinite(price)) return;
    const entry = {
      name: item.querySelector('.price-item__name').textContent.trim(),
      price,
      perUnit: item.hasAttribute('data-per-unit'),
      formOption: item.dataset.form || '',
    };
    catalog.set(id, entry);
    el.treatment.add(new Option(entry.name + (entry.perUnit ? ' (por pieza)' : ''), id));
  });

  const formatEuro = (value) =>
    String(Math.round(value)).replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ' €';

  function readQty() {
    const qty = Math.floor(Number(el.qty.value));
    if (!Number.isFinite(qty) || qty < 1) return 1;
    return Math.min(qty, MAX_QTY);
  }

  function render() {
    const entry = catalog.get(el.treatment.value);
    el.qtyField.hidden = !entry || !entry.perUnit;
    el.insuranceNote.hidden = el.insurance.value !== 'si';

    if (!entry) {
      el.empty.hidden = false;
      el.filled.hidden = true;
      el.cta.dataset.treatment = '';
      return;
    }

    const qty = entry.perUnit ? readQty() : 1;
    const total = entry.price * qty;
    const months = Number(el.months.value) || 1;

    el.price.textContent = formatEuro(total);
    el.monthly.textContent = months > 1
      ? `Aproximadamente ${formatEuro(Math.ceil(total / months))}/mes durante ${months} meses, sin intereses. Financiación sujeta a aprobación.`
      : 'Pago único. También puedes financiarlo hasta 60 meses sin intereses, sujeto a aprobación.';
    el.empty.hidden = true;
    el.filled.hidden = false;
    el.cta.dataset.treatment = entry.formOption;
  }

  form.addEventListener('input', render);
  form.addEventListener('change', (event) => {
    if (event.target === el.qty) el.qty.value = readQty();
    render();
  });

  render();
})();
