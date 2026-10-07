/**
 * Orientación rápida: 1–2 preguntas que sugieren por dónde empezar.
 * Usa los precios de la lista HTML y rellena el formulario de cita.
 * No es un diagnóstico: el resultado lo deja claro.
 */
(function () {
  'use strict';

  const root = document.querySelector('[data-finder]');
  const AEOD = window.AEOD;
  if (!root || !AEOD) return;

  const RESULTS = {
    urgencia: {
      title: 'Atención de urgencia',
      text: 'Si tienes dolor, inflamación o has sufrido un golpe, llámanos: atendemos urgencias las 24 horas, los 365 días del año.',
      form: 'Urgencia dental',
      urgent: true,
    },
    implantes: {
      title: 'Implantes dentales',
      text: 'Para reponer uno o varios dientes, el implante es la solución más natural y duradera. Primero valoramos tu hueso con un TAC 3D.',
      priceId: 'implante', modal: 'modal-implantes', form: 'Implantes dentales',
    },
    invisalign: {
      title: 'Ortodoncia invisible (Invisalign)',
      text: 'Alineadores transparentes y removibles. Antes de empezar verás una simulación 3D del resultado.',
      priceId: 'invisalign-lite', modal: 'modal-ortodoncia', form: 'Ortodoncia / Invisalign',
    },
    brackets: {
      title: 'Ortodoncia con brackets',
      text: 'Brackets estéticos de zafiro o cerámica, o metálicos de alta eficacia. Al ser fijos, no dependen de que te acuerdes de llevarlos.',
      priceId: 'brackets', modal: 'modal-ortodoncia', form: 'Ortodoncia / Invisalign',
    },
    blanqueamiento: {
      title: 'Blanqueamiento dental',
      text: 'Blanqueamiento profesional en consulta, en una sola sesión de 45 a 60 minutos. El resultado depende del punto de partida de cada paciente.',
      priceId: 'blanqueamiento', modal: 'modal-implantes', form: 'Estética dental / Carillas',
    },
    carillas: {
      title: 'Carillas de porcelana',
      text: 'Con diseño digital de sonrisa (DSD) ves el resultado antes de empezar, con un desgaste mínimo del esmalte.',
      priceId: 'carilla', modal: 'modal-implantes', form: 'Estética dental / Carillas',
    },
    revision: {
      title: 'Primera visita y revisión',
      text: 'Empieza por una primera visita sin coste: exploración, radiografía panorámica y un plan de tratamiento claro.',
      priceId: 'limpieza', modal: 'modal-general', form: 'Revisión general',
    },
  };

  const $ = (sel) => root.querySelector(sel);
  const panel = $('[data-finder-panel]');
  const steps = Array.from(root.querySelectorAll('[data-finder-step]'));
  const result = $('[data-finder-result]');
  const el = {
    title: $('[data-finder-title]'),
    text: $('[data-finder-text]'),
    price: $('[data-finder-price]'),
    cta: $('[data-finder-cta]'),
    call: $('[data-finder-call]'),
    more: $('[data-finder-more]'),
  };

  root.querySelector('[data-finder-nojs]').hidden = true;
  panel.hidden = false;

  const history = [];
  let current = null;

  function priceFor(id) {
    const item = id && document.querySelector(`.price-item[data-id="${id}"]`);
    if (!item) return '';
    const unit = item.hasAttribute('data-per-unit') ? ' por pieza' : '';
    return `Precio orientativo: desde ${AEOD.formatEuro(Number(item.dataset.price))}${unit}.`;
  }

  function showStep(name, { focus = true } = {}) {
    result.hidden = true;
    steps.forEach((step) => { step.hidden = step.dataset.finderStep !== name; });
    if (focus) {
      const question = root.querySelector(`[data-finder-step="${name}"] .finder__question`);
      if (question) question.focus();
    }
  }

  function showResult(key) {
    const data = RESULTS[key];
    if (!data) return;
    current = data;
    steps.forEach((step) => { step.hidden = true; });

    el.title.textContent = data.title;
    el.text.textContent = data.text;
    const price = priceFor(data.priceId);
    el.price.textContent = price;
    el.price.hidden = !price;

    el.call.hidden = !data.urgent;
    el.cta.classList.toggle('btn--solid', !data.urgent);
    el.cta.classList.toggle('btn--outline', !!data.urgent);
    el.more.hidden = !data.modal;
    if (data.modal) el.more.dataset.modalOpen = data.modal;
    el.cta.dataset.treatment = data.form;

    result.hidden = false;
    el.title.focus();
  }

  root.addEventListener('click', (event) => {
    const next = event.target.closest('[data-finder-next]');
    const choice = event.target.closest('[data-finder-choice]');
    const visible = steps.find((step) => !step.hidden);

    if (next) {
      history.push(visible.dataset.finderStep);
      showStep(next.dataset.finderNext);
    } else if (choice) {
      if (visible) history.push(visible.dataset.finderStep);
      showResult(choice.dataset.finderChoice);
    } else if (event.target.closest('[data-finder-back]')) {
      showStep(history.pop() || 'goal');
    } else if (event.target.closest('[data-finder-restart]')) {
      history.length = 0;
      current = null;
      showStep('goal');
    } else if (event.target.closest('[data-finder-cta]') && current) {
      AEOD.prefillContact({
        treatment: current.form,
        message: `Me interesa: ${current.title}. (Orientación desde la web)`,
      });
    }
  });

  showStep('goal', { focus: false });
})();
