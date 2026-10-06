/**
 * Formulario de cita.
 * Responsabilidades separadas:
 *   1. Validación (reglas puras + pintado de errores accesible)
 *   2. Envío a la API (AEOD.api):
 *      - con un horario elegido en el paso 1 (booking.js) reserva la cita (POST /citas);
 *        si otra persona se adelantó (409), se recargan los horarios y se pide elegir otro
 *      - si no (urgencia, "prefiero que me llaméis", API sin catálogo) envía un contacto (POST /contacto)
 *   3. Estados de UI: carga, éxito y error
 *   4. Ayudas: contador de caracteres, borrador en la pestaña y
 *      relleno desde otros módulos (estimador, orientación, CTA)
 *
 * Qué días y horas hay libres lo decide siempre el backend, nunca este archivo.
 */
(function () {
  'use strict';

  const form = document.querySelector('[data-contact-form]');
  const AEOD = window.AEOD || {};
  if (!form) return;

  const DRAFT_KEY = 'aeod-contact-draft';
  const MAX_DAYS_AHEAD = 180;
  const CONTACT_FALLBACK =
    'Llámanos al <a href="tel:+34900000000">900 00 00 00</a> o escríbenos a ' +
    '<a href="mailto:atencion@aeod.es">atencion@aeod.es</a>.';

  const submitButton = form.querySelector('[data-submit]');
  const submitLabel = form.querySelector('[data-submit-label]');
  const summary = form.querySelector('[data-form-status]');
  const charCount = form.querySelector('[data-char-count]');
  const booking = () => AEOD.booking;
  const isBooking = () => Boolean(booking() && booking().mode() === 'cita');
  const idleLabel = () => (isBooking() ? 'Confirmar cita' : 'Enviar solicitud');

  /* ── Fechas (día preferido) ── */
  const today = () => (AEOD.clinicNow ? AEOD.clinicNow().isoDate : new Date().toISOString().slice(0, 10));
  const parseISO = (iso) => {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(Date.UTC(y, m - 1, d));
  };
  const addDays = (iso, days) => {
    const date = parseISO(iso);
    date.setUTCDate(date.getUTCDate() + days);
    return date.toISOString().slice(0, 10);
  };

  form.elements.date.min = today();
  form.elements.date.max = addDays(today(), MAX_DAYS_AHEAD);

  /* ── 1. Validación ── */
  const RULES = {
    name(value) {
      if (!value) return 'Escribe tu nombre.';
      if (value.length < 2) return 'El nombre debe tener al menos 2 caracteres.';
      if (!/^[\p{L}][\p{L}\s'.-]*$/u.test(value)) return 'El nombre solo puede contener letras, espacios y guiones.';
      return '';
    },
    phone(value) {
      if (!value) return 'Escribe un teléfono de contacto.';
      if (!/^\+?[\d\s().-]+$/.test(value)) return 'El teléfono solo puede contener números, espacios y el prefijo +.';
      const digits = value.replace(/\D/g, '');
      if (digits.length < 9 || digits.length > 15) return 'Revisa el teléfono: debe tener entre 9 y 15 dígitos.';
      return '';
    },
    email(value) {
      if (!value) return 'Escribe tu correo electrónico.';
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value)) return 'Revisa el correo: debe tener el formato nombre@dominio.com.';
      return '';
    },
    treatment(value) {
      return value ? '' : 'Selecciona el tratamiento que te interesa.';
    },
    date(value) {
      if (!value) return '';
      if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return 'Revisa la fecha.';
      if (value < today()) return 'Elige una fecha a partir de hoy.';
      if (value > addDays(today(), MAX_DAYS_AHEAD)) return 'Elige una fecha dentro de los próximos 6 meses.';
      return '';
    },
    message(value) {
      return value.length > 1000 ? 'El mensaje no puede superar los 1.000 caracteres.' : '';
    },
  };

  const fields = Object.keys(RULES).map((name) => form.elements[name]);
  const touched = new Set();

  function getData() {
    return {
      name: form.elements.name.value.trim(),
      phone: form.elements.phone.value.trim(),
      email: form.elements.email.value.trim(),
      treatment: form.elements.treatment.value,
      date: form.elements.date.value,
      slot: form.elements.slot.value,
      message: form.elements.message.value.trim(),
    };
  }

  function showFieldError(field, message) {
    const error = form.querySelector(`[data-error-for="${field.name}"]`);
    if (error) error.textContent = message;
    if (message) field.setAttribute('aria-invalid', 'true');
    else field.removeAttribute('aria-invalid');
  }

  function validateField(field) {
    const data = getData();
    const message = RULES[field.name](data[field.name], data);
    showFieldError(field, message);
    return message;
  }

  fields.forEach((field) => {
    field.addEventListener('blur', () => {
      touched.add(field.name);
      validateField(field);
    });
    // Selects y fechas: el cambio ya es una decisión, se valida al momento
    field.addEventListener('change', () => {
      if (field.tagName === 'SELECT' || field.type === 'date') {
        touched.add(field.name);
        validateField(field);
      }
    });
    field.addEventListener('input', () => {
      if (touched.has(field.name)) validateField(field);
    });
  });

  /* ── 4a. Contador de caracteres ── */
  function updateCount() {
    if (charCount) charCount.textContent = form.elements.message.value.length.toLocaleString('es-ES');
  }
  form.elements.message.addEventListener('input', updateCount);

  /* ── 4b. Borrador en esta pestaña (sessionStorage, se borra al enviar) ── */
  const DRAFT_FIELDS = ['name', 'phone', 'email', 'treatment', 'dentist', 'date', 'slot', 'message'];
  let draftTimer = null;
  function saveDraft() {
    if (!AEOD.session) return;
    window.clearTimeout(draftTimer);
    draftTimer = window.setTimeout(() => {
      const draft = {};
      DRAFT_FIELDS.forEach((name) => { draft[name] = form.elements[name].value; });
      AEOD.session.set(DRAFT_KEY, draft);
    }, 300);
  }
  function restoreDraft() {
    const draft = AEOD.session && AEOD.session.get(DRAFT_KEY);
    if (!draft) return;
    DRAFT_FIELDS.forEach((name) => {
      if (typeof draft[name] !== 'string' || form.elements[name].value) return;
      // Tratamiento y odontólogo dependen del catálogo de la API, que llega después
      if (name === 'treatment' && booking()) booking().selectTreatment(draft.treatment, { silent: true });
      else if (name === 'dentist' && booking()) booking().restoreDentist(draft.dentist);
      else form.elements[name].value = draft[name];
    });
  }
  form.addEventListener('input', saveDraft);
  form.addEventListener('change', saveDraft);
  // Guardado inmediato si se cierra o recarga la pestaña antes del retardo
  window.addEventListener('pagehide', () => {
    if (!draftTimer || !AEOD.session) return;
    window.clearTimeout(draftTimer);
    const draft = {};
    DRAFT_FIELDS.forEach((name) => { draft[name] = form.elements[name].value; });
    AEOD.session.set(DRAFT_KEY, draft);
  });
  restoreDraft();
  updateCount();

  /* ── 3. Estados de UI ── */
  function setLoading(loading) {
    submitButton.disabled = loading;
    submitButton.classList.toggle('is-loading', loading);
    form.setAttribute('aria-busy', String(loading));
    submitLabel.textContent = loading ? (isBooking() ? 'Reservando…' : 'Enviando…') : idleLabel();
  }

  form.addEventListener('aeod:booking-mode', () => {
    if (!submitButton.disabled) submitLabel.textContent = idleLabel();
  });
  // Errores de campo que detecta el paso 1 al consultar horarios (p. ej. una fecha fuera de rango)
  form.addEventListener('aeod:booking-field-error', (event) => {
    const field = form.elements[event.detail.field];
    if (field) showFieldError(field, event.detail.message);
  });

  function showSummary(type, html) {
    summary.className = `contact-form__summary is-${type}`;
    summary.innerHTML = html;
    summary.hidden = false;
    summary.focus();
  }

  function clearSummary() {
    summary.hidden = true;
    summary.textContent = '';
  }

  /* ── 2. Envío ── */
  const SLOT_LABELS = { manana: 'mañana', tarde: 'tarde' };
  const ESTADOS = { PENDIENTE: 'Pendiente de confirmación', CONFIRMADA: 'Confirmada' };
  // Campos de la API → campos del formulario, para pintar los errores que devuelve el backend
  const API_FIELDS = {
    nombre: 'name', telefono: 'phone', email: 'email', tratamiento: 'treatment', tratamiento_id: 'treatment',
    fecha: 'date', mensaje: 'message',
  };
  const API_SLOT_FIELDS = ['hora_inicio', 'odontologo_id'];

  /** Mensaje final: añade lo que la API de contacto no guarda en un campo propio (día, franja). */
  function buildMessage(data, extras) {
    const notes = extras.filter(Boolean).join(' ');
    const text = [notes, data.message].filter(Boolean).join('\n');
    return text.slice(0, 1000);
  }

  async function send(data) {
    const { api } = AEOD;
    if (isBooking()) {
      const respuesta = await api.reservarCita({
        ...booking().request(),
        nombre: data.name,
        telefono: data.phone,
        email: data.email,
        mensaje: data.message || null,
      });
      return { kind: 'cita', respuesta };
    }
    const slot = SLOT_LABELS[data.slot] ? `Franja preferida: ${SLOT_LABELS[data.slot]}.` : '';
    await api.enviarContacto({
      nombre: data.name,
      telefono: data.phone,
      email: data.email,
      tratamiento: data.treatment,
      mensaje: buildMessage(data, [data.date ? `Día preferido: ${data.date}.` : '', slot]),
    });
    return { kind: 'contacto' };
  }

  const escape = (text) => {
    const div = document.createElement('div');
    div.textContent = text == null ? '' : String(text);
    return div.innerHTML;
  };

  /** Código de la última cita reservada en esta página: con él, el paciente puede cancelarla aquí mismo. */
  let lastBookingCode = null;

  /** Confirmación de la cita con los datos que devuelve el backend. */
  function bookingConfirmation(respuesta) {
    const cita = respuesta.datos || {};
    const [y, m, d] = String(cita.fecha || '').split('-').map(Number);
    const fecha = y ? new Intl.DateTimeFormat('es-ES', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })
      .format(new Date(Date.UTC(y, m - 1, d))) : '';
    const rows = [
      ['Tratamiento', cita.tratamiento],
      ['Día', fecha.charAt(0).toLocaleUpperCase('es') + fecha.slice(1)],
      ['Hora', cita.hora_inicio && cita.hora_fin ? `${cita.hora_inicio} – ${cita.hora_fin}` : cita.hora_inicio],
      ['Odontólogo/a', cita.odontologo],
      ['Estado', ESTADOS[cita.estado] || cita.estado],
    ].filter(([, value]) => value);
    lastBookingCode = cita.codigo || null;
    return `<strong>${escape(respuesta.message || 'Tu cita está reservada.')}</strong>`
      + `<dl class="booking-confirmation">${rows.map(([label, value]) =>
        `<div><dt>${label}</dt><dd>${escape(value)}</dd></div>`).join('')}</dl>`
      + (lastBookingCode
        ? `<div class="booking-cancel" data-booking-cancel>${CANCEL_OFFER}</div>`
        : '<p>Si no puedes venir, avísanos llamando al <a href="tel:+34900000000">900 00 00 00</a>.</p>');
  }

  /* ── 2b. Cancelar la cita recién reservada (POST /citas/cancelacion) ── */
  const CANCEL_OFFER =
    '<p>¿No puedes venir? Cancélala aquí o llámanos al <a href="tel:+34900000000">900 00 00 00</a>.</p>'
    + '<div class="booking-cancel__actions">'
    + '<button type="button" class="btn btn--outline btn--sm" data-cancel-start>Cancelar esta cita</button></div>';
  const CANCEL_QUESTION =
    '<p><strong>¿Seguro que quieres cancelar esta cita?</strong> El horario quedará libre para otra persona.</p>'
    + '<div class="booking-cancel__actions">'
    + '<button type="button" class="btn btn--solid btn--sm" data-cancel-confirm>Sí, cancelar la cita</button>'
    + '<button type="button" class="btn btn--outline btn--sm" data-cancel-keep>No, mantenerla</button></div>';

  summary.addEventListener('click', async (event) => {
    const button = event.target.closest('button');
    const box = summary.querySelector('[data-booking-cancel]');
    if (!button || !box) return;

    if (button.hasAttribute('data-cancel-start')) {
      box.innerHTML = CANCEL_QUESTION;
      box.querySelector('[data-cancel-confirm]').focus();
    } else if (button.hasAttribute('data-cancel-keep')) {
      box.innerHTML = CANCEL_OFFER;
      box.querySelector('[data-cancel-start]').focus();
    } else if (button.hasAttribute('data-cancel-confirm')) {
      box.querySelectorAll('button').forEach((b) => { b.disabled = true; });
      button.textContent = 'Cancelando…';
      const { api } = AEOD;
      try {
        await api.cancelarCita(lastBookingCode);
        lastBookingCode = null;
        showSummary('success', '<strong>Tu cita se ha cancelado.</strong> Ese horario vuelve a estar libre. '
          + 'Si quieres otro día u otra hora, puedes reservar de nuevo cuando quieras.');
      } catch (error) {
        // 409: ya no se puede cancelar online (muy poca antelación o ya no está activa); el mensaje lo da la API
        const known = api && error instanceof api.ApiRequestError && (error.status === 409 || error.status === 404);
        box.innerHTML = `<p class="booking-cancel__error" role="alert">${known
          ? escape(error.message)
          : 'No hemos podido cancelar la cita. Inténtalo de nuevo en unos minutos.'} `
          + 'También puedes llamarnos al <a href="tel:+34900000000">900 00 00 00</a>.</p>'
          + (known ? '' : CANCEL_OFFER.replace(/^<p>.*?<\/p>/, ''));
        const retry = box.querySelector('[data-cancel-start]');
        if (retry) retry.focus();
      }
    }
  });

  function showApiFieldErrors(errores) {
    const invalid = [];
    Object.entries(errores).forEach(([apiField, message]) => {
      if (API_SLOT_FIELDS.includes(apiField) && booking()) {
        booking().showError(message);
        invalid.push({ focus: () => booking().focus() });
        return;
      }
      const field = form.elements[API_FIELDS[apiField]];
      if (!field) return;
      showFieldError(field, message);
      invalid.push(field);
    });
    return invalid;
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearSummary();

    const invalid = fields.filter((field) => {
      touched.add(field.name);
      return validateField(field);
    });
    // Paso 1: en una reserva hace falta elegir uno de los horarios disponibles
    const missingSlot = booking() ? booking().validate() : '';
    const stepOne = invalid.filter((field) => field === form.elements.treatment || field === form.elements.date);

    if (stepOne.length) {
      stepOne[0].focus();
      return;
    }
    if (missingSlot) {
      booking().focus();
      return;
    }
    if (invalid.length) {
      invalid[0].focus();
      return;
    }

    const { api } = AEOD;
    setLoading(true);
    try {
      const { kind, respuesta } = await send(getData());
      form.reset();
      touched.clear();
      updateCount();
      AEOD.session.remove(DRAFT_KEY);
      if (booking()) booking().reset();
      showSummary('success', kind === 'cita'
        ? bookingConfirmation(respuesta)
        : '<strong>Hemos recibido tu solicitud.</strong> Te llamaremos para ayudarte y, si lo necesitas, darte cita.');
    } catch (error) {
      const notConfigured = api && error instanceof api.ApiNotConfiguredError;
      const rejected = api && error instanceof api.ApiRequestError && error.status === 400;
      const taken = api && error instanceof api.ApiRequestError && error.status === 409;
      const invalidFields = rejected ? showApiFieldErrors(error.errores) : [];
      if (taken && booking()) {
        // Otra persona reservó ese horario antes: nueva lista y aviso junto a los horarios
        await booking().conflict(error.message);
      } else if (invalidFields.length) {
        showSummary('error', '<strong>Revisa los campos marcados.</strong>');
        invalidFields[0].focus();
      } else {
        showSummary('error', notConfigured
          ? `<strong>Por ahora no podemos recibir solicitudes online.</strong> ${CONTACT_FALLBACK}`
          : `<strong>No hemos podido enviar tu solicitud.</strong> Inténtalo de nuevo en unos minutos. ${CONTACT_FALLBACK}`);
      }
    } finally {
      setLoading(false);
    }
  });

  /* ── 4c. Relleno desde otros módulos ── */
  function selectTreatment(value) {
    const select = form.elements.treatment;
    if (booking()) {
      // booking.js traduce "Implantes dentales" al tratamiento de la API y recarga los horarios
      booking().selectTreatment(value);
    } else if (value && Array.from(select.options).some((option) => option.value === value)) {
      select.value = value;
    }
    if (touched.has('treatment')) validateField(select);
  }

  // Enlaces "Solicitar cita" con data-treatment
  document.addEventListener('click', (event) => {
    const link = event.target.closest('a[data-treatment]');
    if (link) selectTreatment(link.dataset.treatment);
  });

  // Estimador y orientación: tratamiento + mensaje (solo si el paciente no ha escrito el suyo)
  let autoMessage = '';
  document.addEventListener('aeod:prefill-contact', (event) => {
    const { treatment, message } = event.detail || {};
    selectTreatment(treatment);
    const textarea = form.elements.message;
    if (message && (!textarea.value.trim() || textarea.value === autoMessage)) {
      textarea.value = message.slice(0, 1000);
      autoMessage = textarea.value;
      updateCount();
    }
    saveDraft();
  });
})();
