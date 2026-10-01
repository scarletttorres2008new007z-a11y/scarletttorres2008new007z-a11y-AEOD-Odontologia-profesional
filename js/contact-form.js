/**
 * Formulario de cita.
 * Responsabilidades separadas:
 *   1. Validación (reglas puras + pintado de errores accesible)
 *   2. Envío a la API (AEOD.api): con día preferido y tratamiento del catálogo
 *      se envía como solicitud de cita (POST /citas); si no, como contacto (POST /contacto).
 *      Una solicitud de cita NO confirma la cita: la clínica llama para confirmarla.
 *   3. Estados de UI: carga, éxito y error
 *   4. Ayudas: contador de caracteres, borrador en la pestaña y
 *      relleno desde otros módulos (estimador, orientación, CTA)
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
  const idleLabel = submitLabel.textContent;

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
  const weekday = (iso) => parseISO(iso).getUTCDay();

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
      if (weekday(value) === 0) return 'Los domingos solo atendemos urgencias por teléfono. Elige de lunes a sábado.';
      return '';
    },
    slot(value, data) {
      if (value === 'tarde' && data.date && weekday(data.date) === 6) {
        return 'Los sábados atendemos de 10:00 a 14:00. Elige la mañana o cambia el día.';
      }
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
      // El día condiciona la franja: se revalida en cuanto cambia
      if (field.name === 'date' && touched.has('slot')) validateField(form.elements.slot);
    });
  });

  /* ── 4a. Contador de caracteres ── */
  function updateCount() {
    if (charCount) charCount.textContent = form.elements.message.value.length.toLocaleString('es-ES');
  }
  form.elements.message.addEventListener('input', updateCount);

  /* ── 4b. Borrador en esta pestaña (sessionStorage, se borra al enviar) ── */
  const DRAFT_FIELDS = ['name', 'phone', 'email', 'treatment', 'date', 'slot', 'message'];
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
      if (typeof draft[name] === 'string' && !form.elements[name].value) form.elements[name].value = draft[name];
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
    submitLabel.textContent = loading ? 'Enviando…' : idleLabel;
  }

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
  // Campos de la API → campos del formulario, para pintar los errores que devuelve el backend
  const API_FIELDS = {
    nombre: 'name', telefono: 'phone', email: 'email', tratamiento: 'treatment', tratamiento_id: 'treatment',
    fecha_preferida: 'date', hora_preferida: 'slot', mensaje: 'message',
  };

  function selectedOption() {
    const select = form.elements.treatment;
    return select.options[select.selectedIndex];
  }

  /** Mensaje final: añade lo que la API no guarda en un campo propio (franja, interés, día). */
  function buildMessage(data, extras) {
    const notes = extras.filter(Boolean).join(' ');
    const text = [notes, data.message].filter(Boolean).join('\n');
    return text.slice(0, 1000);
  }

  async function send(data) {
    const { api } = AEOD;
    const option = selectedOption();
    // El formulario agrupa tratamientos ("Implantes dentales"); data-tratamiento indica su nombre en la API
    const apiName = option.dataset.tratamiento || '';
    const tratamientoId = data.date && apiName && AEOD.tratamientoId ? await AEOD.tratamientoId(apiName) : null;
    const slot = SLOT_LABELS[data.slot] ? `Franja preferida: ${SLOT_LABELS[data.slot]}.` : '';

    if (tratamientoId) {
      await api.solicitarCita({
        nombre: data.name,
        telefono: data.phone,
        email: data.email,
        tratamiento_id: tratamientoId,
        fecha_preferida: data.date,
        hora_preferida: null,
        mensaje: buildMessage(data, [apiName !== data.treatment ? `Interés: ${data.treatment}.` : '', slot]),
      });
      return 'cita';
    }
    await api.enviarContacto({
      nombre: data.name,
      telefono: data.phone,
      email: data.email,
      tratamiento: data.treatment,
      mensaje: buildMessage(data, [data.date ? `Día preferido: ${data.date}.` : '', slot]),
    });
    return 'contacto';
  }

  function showApiFieldErrors(errores) {
    const invalid = [];
    Object.entries(errores).forEach(([apiField, message]) => {
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

    if (invalid.length) {
      invalid[0].focus();
      return;
    }

    const { api } = AEOD;
    setLoading(true);
    try {
      const kind = await send(getData());
      form.reset();
      touched.clear();
      updateCount();
      AEOD.session.remove(DRAFT_KEY);
      showSummary('success', kind === 'cita'
        ? '<strong>Hemos recibido tu solicitud de cita.</strong> Aún no está confirmada: te llamaremos para confirmar el día y la hora.'
        : '<strong>Hemos recibido tu solicitud.</strong> Te llamaremos para ayudarte y, si lo necesitas, darte cita.');
    } catch (error) {
      const notConfigured = api && error instanceof api.ApiNotConfiguredError;
      const rejected = api && error instanceof api.ApiRequestError && error.status === 400;
      const invalidFields = rejected ? showApiFieldErrors(error.errores) : [];
      if (invalidFields.length) {
        showSummary('error', '<strong>Revisa los campos marcados.</strong>');
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
    if (!value || !Array.from(select.options).some((option) => option.value === value)) return;
    select.value = value;
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
