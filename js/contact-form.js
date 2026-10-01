/**
 * Formulario de cita.
 * Responsabilidades separadas:
 *   1. Validación (reglas puras + pintado de errores accesible)
 *   2. Envío (delegado en AEOD.api.submitContactRequest)
 *   3. Estados de UI: carga, éxito y error
 */
(function () {
  'use strict';

  const form = document.querySelector('[data-contact-form]');
  if (!form) return;

  const CONTACT_FALLBACK =
    'Llámanos al <a href="tel:+34900000000">900 00 00 00</a> o escríbenos a ' +
    '<a href="mailto:atencion@aeod.es">atencion@aeod.es</a>.';

  const submitButton = form.querySelector('[data-submit]');
  const submitLabel = form.querySelector('[data-submit-label]');
  const summary = form.querySelector('[data-form-status]');
  const idleLabel = submitLabel.textContent;

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
    const message = RULES[field.name](getData()[field.name]);
    showFieldError(field, message);
    return message;
  }

  fields.forEach((field) => {
    field.addEventListener('blur', () => {
      touched.add(field.name);
      validateField(field);
    });
    field.addEventListener('input', () => {
      if (touched.has(field.name)) validateField(field);
    });
  });

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

    const { api } = window.AEOD || {};
    setLoading(true);
    try {
      await api.submitContactRequest(getData(), { endpoint: form.dataset.endpoint });
      form.reset();
      touched.clear();
      showSummary('success',
        '<strong>Hemos recibido tu solicitud.</strong> Te llamaremos para confirmar tu cita.');
    } catch (error) {
      const notConfigured = api && error instanceof api.ApiNotConfiguredError;
      showSummary('error', notConfigured
        ? `<strong>Por ahora no podemos recibir solicitudes online.</strong> ${CONTACT_FALLBACK}`
        : `<strong>No hemos podido enviar tu solicitud.</strong> Inténtalo de nuevo en unos minutos. ${CONTACT_FALLBACK}`);
    } finally {
      setLoading(false);
    }
  });

  /* ── Preselección del tratamiento desde otros CTA (data-treatment) ── */
  document.addEventListener('click', (event) => {
    const link = event.target.closest('a[data-treatment]');
    const value = link && link.dataset.treatment;
    if (!value) return;
    const select = form.elements.treatment;
    const exists = Array.from(select.options).some((option) => option.value === value);
    if (!exists) return;
    select.value = value;
    if (touched.has('treatment')) validateField(select);
  });
})();
