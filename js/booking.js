/**
 * Reserva online: paso 1 del formulario de cita ("Elige tu cita").
 *
 * El navegador NO decide qué horario está libre. Pide los horarios a la API
 * (GET /disponibilidad) y solo muestra los que recibe; al confirmar, el backend
 * vuelve a comprobar que el horario sigue libre (409 si alguien se adelantó).
 *
 *   tratamiento + odontólogo (opcional) + día + franja → horarios disponibles → el paciente elige uno
 *   sin horarios → aviso, próximas opciones y "Encontrar el horario más cercano"
 *   "Prefiero que me llaméis", "Urgencia dental" u "Otro" → el formulario envía un contacto
 *
 * Si la API no está configurada o no responde, el formulario se queda con sus opciones
 * estáticas y funciona como solicitud de contacto: nunca simula una reserva.
 *
 * Expone AEOD.booking para contact-form.js.
 */
(function () {
  'use strict';

  const form = document.querySelector('[data-contact-form]');
  const AEOD = window.AEOD || {};
  const { api } = AEOD;
  if (!form || !api) return;

  const el = {
    treatment: form.elements.treatment,
    dentist: form.elements.dentist,
    date: form.elements.date,
    slot: form.elements.slot,
    dateHint: form.querySelector('[data-date-hint]'),
    bookingOnly: form.querySelectorAll('[data-booking-only]'),
    panel: form.querySelector('[data-slots]'),
    status: form.querySelector('[data-slots-status]'),
    list: form.querySelector('[data-slots-list]'),
    error: form.querySelector('[data-error-for="horario"]'),
    nearest: form.querySelector('[data-slots-nearest]'),
    callback: form.querySelector('[data-slots-callback]'),
    callbackNote: form.querySelector('[data-callback-note]'),
    callbackUndo: form.querySelector('[data-callback-undo]'),
    pick: form.querySelector('[data-booking-pick]'),
    pickValue: form.querySelector('[data-booking-pick-value]'),
    pickMeta: form.querySelector('[data-booking-pick-meta]'),
  };

  const FRANJAS = { manana: 'MANANA', tarde: 'TARDE' };
  const HINT_BOOKING = 'Te mostramos los horarios libres de ese día.';
  const HINT_CONTACT = el.dateHint.textContent;
  // Solo para agrupar en pantalla; qué horas existen lo decide el backend
  const AFTERNOON_FROM = '14:00';

  const state = {
    tratamientos: new Map(),   // nombre → tratamiento de la API
    odontologos: [],
    aliases: new Map(),        // opción del formulario ("Implantes dentales") → nombre en la API
    online: false,             // hay catálogo de la API: se puede reservar
    callback: false,           // el paciente prefiere que le llamen
    view: null,                // { kind: 'dia' | 'proximos', fecha }
    results: [],
    selected: null,
    request: 0,
    pendingTreatment: '',
    pendingDentist: '',
  };

  /* ── Formato ── */
  const normalize = (text) => String(text || '').trim().toLocaleLowerCase('es')
    .normalize('NFD').replace(/[̀-ͯ]/g, '');
  const capitalize = (text) => text.charAt(0).toLocaleUpperCase('es') + text.slice(1);
  const toDate = (iso) => {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(Date.UTC(y, m - 1, d));
  };
  const longDate = new Intl.DateTimeFormat('es-ES', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' });
  const shortDate = new Intl.DateTimeFormat('es-ES', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });
  const formatLongDate = (iso) => capitalize(longDate.format(toDate(iso)));
  const formatShortDate = (iso) => capitalize(shortDate.format(toDate(iso)).replace(/\./g, '').replace(',', ''));
  const treatmentLabel = (t) => (t.duracion_minutos ? `${t.nombre} — ${t.duracion_minutos} min` : t.nombre);

  /* ── Modo: reserva online o contacto ── */
  function currentTreatment() {
    return state.tratamientos.get(el.treatment.value) || null;
  }

  function mode() {
    return state.online && currentTreatment() && !state.callback ? 'cita' : 'contacto';
  }

  let lastMode = null;
  function syncMode() {
    const booking = state.online && Boolean(currentTreatment());
    el.bookingOnly.forEach((node) => { node.hidden = !booking; });
    el.panel.hidden = !booking || state.callback;
    el.callbackNote.hidden = !booking || !state.callback;
    el.dateHint.textContent = booking && !state.callback ? HINT_BOOKING : HINT_CONTACT;
    renderPick();
    const current = mode();
    if (current !== lastMode) {
      lastMode = current;
      form.dispatchEvent(new CustomEvent('aeod:booking-mode', { detail: { mode: current } }));
    }
  }

  /* ── Catálogo (tratamientos con duración y odontólogos) ── */
  function rememberAliases() {
    Array.from(el.treatment.options).forEach((option) => {
      if (option.dataset.tratamiento) state.aliases.set(option.value, option.dataset.tratamiento);
    });
  }

  function buildTreatmentOptions(tratamientos) {
    const previous = el.treatment.value;
    const contactOnly = Array.from(el.treatment.options).filter((option) => option.value && !option.dataset.tratamiento);
    const placeholder = el.treatment.options[0];
    const options = tratamientos.map((t) => {
      const option = new Option(treatmentLabel(t), t.nombre);
      option.dataset.tratamientoId = String(t.id);
      return option;
    });
    el.treatment.replaceChildren(placeholder, ...options, ...contactOnly);
    if (previous) selectTreatment(previous, { silent: true });
  }

  function buildDentistOptions() {
    const t = currentTreatment();
    const previous = state.pendingDentist || el.dentist.value;
    const placeholder = el.dentist.options[0];
    const ids = t && t.odontologo_ids && t.odontologo_ids.length ? new Set(t.odontologo_ids) : null;
    const options = state.odontologos
      .filter((o) => !ids || ids.has(o.id))
      .map((o) => new Option(o.nombre, String(o.id)));
    el.dentist.replaceChildren(placeholder, ...options);
    el.dentist.value = options.some((o) => o.value === previous) ? previous : '';
    if (state.pendingDentist && t) state.pendingDentist = '';
  }

  /** Selecciona un tratamiento por su nombre en la API o por la opción del formulario ("Revisión general"). */
  function selectTreatment(value, { silent = false } = {}) {
    if (!value) return false;
    const options = Array.from(el.treatment.options);
    const target = options.find((o) => o.value === value)
      || options.find((o) => o.value === state.aliases.get(value))
      || options.find((o) => normalize(o.value) === normalize(value));
    if (!target) {
      // El catálogo de la API todavía no ha llegado: se aplica cuando llegue
      if (!state.online) state.pendingTreatment = value;
      return false;
    }
    el.treatment.value = target.value;
    if (!silent) el.treatment.dispatchEvent(new Event('change', { bubbles: true }));
    return true;
  }

  /* ── Horarios ── */
  function clearSelection() {
    state.selected = null;
    el.list.querySelectorAll('.slot.is-selected').forEach((node) => node.classList.remove('is-selected'));
    renderPick();
  }

  function setStatus(html, type) {
    el.status.className = type ? `slots__status slots__notice is-${type}` : 'slots__status';
    el.status.innerHTML = html;
  }

  function slotNode(horario, index, withDate) {
    const label = document.createElement('label');
    label.className = 'slot';
    const input = document.createElement('input');
    input.type = 'radio';
    input.name = 'horario';
    input.value = String(index);
    const time = document.createElement('span');
    time.className = 'slot__time';
    time.textContent = horario.hora_inicio;
    const meta = document.createElement('span');
    meta.className = 'slot__meta';
    meta.textContent = withDate ? `${formatShortDate(horario.fecha)} · ${horario.odontologo_nombre}` : horario.odontologo_nombre;
    const accessible = document.createElement('span');
    accessible.className = 'visually-hidden';
    accessible.textContent = withDate ? '' : ` (${formatLongDate(horario.fecha)})`;
    label.append(input, time, meta, accessible);
    return label;
  }

  function slotGrid(horarios, offset, withDate) {
    const grid = document.createElement('div');
    grid.className = 'slots__grid';
    horarios.forEach((h, i) => grid.append(slotNode(h, offset + i, withDate)));
    return grid;
  }

  function subhead(text) {
    const p = document.createElement('p');
    p.className = 'slots__subhead';
    p.textContent = text;
    return p;
  }

  function renderDay(data) {
    const t = data.tratamiento;
    const head = document.createElement('p');
    head.className = 'slots__head';
    const strong = document.createElement('strong');
    strong.textContent = formatLongDate(data.fecha);
    head.append(strong, ` · ${treatmentLabel(t)}`);

    const morning = data.horarios.filter((h) => h.hora_inicio < AFTERNOON_FROM);
    const afternoon = data.horarios.filter((h) => h.hora_inicio >= AFTERNOON_FROM);
    const nodes = [head];
    if (morning.length && afternoon.length) {
      nodes.push(subhead('Mañana'), slotGrid(morning, 0, false), subhead('Tarde'), slotGrid(afternoon, morning.length, false));
    } else {
      nodes.push(slotGrid(data.horarios, 0, false));
    }
    el.list.replaceChildren(...nodes);
    const total = data.horarios.length;
    setStatus(`<span class="visually-hidden">${total === 1 ? 'Hay 1 horario libre' : `Hay ${total} horarios libres`}.</span>`);
  }

  function renderOptions(horarios, title) {
    el.list.replaceChildren(...(horarios.length ? [subhead(title), slotGrid(horarios, 0, true)] : []));
  }

  async function load(kind) {
    const t = currentTreatment();
    if (!t || state.callback) return;
    clearSelection();
    showError('');

    if (kind === 'dia' && !el.date.value) {
      state.view = null;
      state.results = [];
      el.list.replaceChildren();
      setStatus('Elige un día para ver sus horarios libres, o busca directamente el más cercano.');
      return;
    }

    const id = ++state.request;
    const params = {
      tratamiento_id: t.id,
      odontologo_id: el.dentist.value,
      franja: FRANJAS[el.slot.value] || '',
    };
    el.panel.setAttribute('aria-busy', 'true');
    el.nearest.disabled = true;
    el.list.replaceChildren();
    setStatus('Buscando horarios libres…');

    try {
      const data = kind === 'dia'
        ? await api.getDisponibilidad({ ...params, fecha: el.date.value })
        : await api.getProximosHorarios({ ...params, desde: el.date.value || '' });
      if (id !== state.request) return;
      state.view = { kind, fecha: el.date.value };

      if (kind === 'dia' && data.horarios.length) {
        state.results = data.horarios;
        renderDay(data);
      } else if (kind === 'dia') {
        state.results = data.proximas_opciones || [];
        setStatus(escapeHtml(data.mensaje), 'empty');
        renderOptions(state.results, 'Próximas opciones');
      } else {
        state.results = data.horarios;
        if (state.results.length) setStatus('');
        else setStatus(escapeHtml(data.mensaje), 'empty');
        renderOptions(state.results, `Horarios más cercanos · ${treatmentLabel(data.tratamiento)}`);
      }
    } catch (error) {
      if (id !== state.request) return;
      handleLoadError(error);
    } finally {
      if (id === state.request) {
        el.panel.removeAttribute('aria-busy');
        el.nearest.disabled = false;
      }
    }
  }

  function handleLoadError(error) {
    const rejected = error instanceof api.ApiRequestError && error.status === 400;
    if (rejected && error.errores.fecha) {
      // Fecha fuera de rango: el mensaje va en el campo día
      el.list.replaceChildren();
      setStatus('');
      form.dispatchEvent(new CustomEvent('aeod:booking-field-error', { detail: { field: 'date', message: error.errores.fecha } }));
      return;
    }
    // La API no responde: se pasa a solicitud de contacto, sin fingir disponibilidad
    state.results = [];
    state.callback = true;
    syncMode();
    el.callbackNote.querySelector('p').textContent =
      'Ahora mismo no podemos mostrarte los horarios. Déjanos tus datos y te llamaremos para darte cita.';
  }

  let timer = null;
  function reload() {
    window.clearTimeout(timer);
    timer = window.setTimeout(() => load('dia'), 250);
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text || '';
    return div.innerHTML;
  }

  /* ── Selección ── */
  function renderPick() {
    const h = state.selected;
    const t = currentTreatment();
    el.pick.hidden = !(h && t && mode() === 'cita');
    if (el.pick.hidden) return;
    el.pickValue.textContent = `${formatLongDate(h.fecha)} · ${h.hora_inicio}–${h.hora_fin}`;
    el.pickMeta.textContent = `${treatmentLabel(t)} · ${h.odontologo_nombre}`;
  }

  el.list.addEventListener('change', (event) => {
    const input = event.target.closest('input[name="horario"]');
    if (!input) return;
    el.list.querySelectorAll('.slot.is-selected').forEach((node) => node.classList.remove('is-selected'));
    input.closest('.slot').classList.add('is-selected');
    state.selected = state.results[Number(input.value)] || null;
    showError('');
    renderPick();
  });

  function showError(message) {
    el.error.textContent = message;
    if (message) el.panel.setAttribute('aria-invalid', 'true');
    else el.panel.removeAttribute('aria-invalid');
  }

  /* ── Eventos ── */
  el.treatment.addEventListener('change', () => {
    if (!state.online) return;
    state.callback = false;
    buildDentistOptions();
    syncMode();
    reload();
  });
  [el.dentist, el.date, el.slot].forEach((field) => field.addEventListener('change', () => {
    if (mode() === 'cita') reload();
  }));
  el.nearest.addEventListener('click', () => load('proximos'));
  el.callback.addEventListener('click', () => {
    state.callback = true;
    clearSelection();
    syncMode();
    el.callbackNote.querySelector('p').textContent = 'Te llamaremos para buscar contigo el mejor horario.';
    el.callbackUndo.focus();
  });
  el.callbackUndo.addEventListener('click', () => {
    state.callback = false;
    syncMode();
    el.panel.focus();
    load('dia');
  });

  /* ── Arranque ── */
  rememberAliases();
  syncMode();

  Promise.resolve(AEOD.catalogo).then((catalogo) => {
    const tratamientos = (catalogo && catalogo.tratamientos) || [];
    if (!tratamientos.length) return; // Sin API: el formulario sigue como contacto
    state.online = true;
    state.odontologos = catalogo.odontologos || [];
    tratamientos.forEach((t) => state.tratamientos.set(t.nombre, t));
    buildTreatmentOptions(tratamientos);
    if (state.pendingTreatment) selectTreatment(state.pendingTreatment, { silent: true });
    state.pendingTreatment = '';
    buildDentistOptions();
    syncMode();
    if (currentTreatment()) load('dia');
  });

  /* ── Para contact-form.js ── */
  AEOD.booking = {
    mode,
    selectTreatment,
    /** Selección de odontólogo guardada en el borrador (se aplica cuando llega el catálogo). */
    restoreDentist(value) {
      state.pendingDentist = value || '';
      if (state.online) buildDentistOptions();
    },
    /** Mensaje de error si falta elegir horario; '' si todo está bien. */
    validate() {
      const message = mode() === 'cita' && !state.selected ? 'Elige uno de los horarios disponibles.' : '';
      showError(message);
      return message;
    },
    showError,
    focus() {
      const first = el.list.querySelector('input[name="horario"]');
      (first || el.panel).focus();
    },
    /** Datos de la cita para POST /citas. */
    request() {
      const h = state.selected;
      return {
        tratamiento_id: currentTreatment().id,
        odontologo_id: h.odontologo_id,
        fecha: h.fecha,
        hora_inicio: h.hora_inicio,
      };
    },
    /** Alguien reservó ese horario antes: se vuelve a pedir la lista y se avisa al paciente. */
    async conflict(message) {
      await load(state.view ? state.view.kind : 'dia');
      setStatus(escapeHtml(message), 'error');
      el.panel.scrollIntoView({ behavior: AEOD.prefersReducedMotion && AEOD.prefersReducedMotion() ? 'auto' : 'smooth', block: 'start' });
      el.panel.focus({ preventScroll: true });
    },
    reset() {
      state.callback = false;
      state.view = null;
      state.results = [];
      clearSelection();
      el.list.replaceChildren();
      setStatus('');
      showError('');
      buildDentistOptions();
      syncMode();
    },
  };
})();
