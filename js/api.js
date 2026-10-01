/**
 * Capa de envío al backend. Punto único para conectar la API real.
 *
 * Para activarlo, define la URL en el atributo data-endpoint del formulario:
 *   <form data-contact-form data-endpoint="https://api.ejemplo.com/citas">
 * Se envía un POST con JSON: { name, phone, email, treatment, message }.
 * Cualquier respuesta 2xx se considera éxito.
 *
 * Mientras no haya endpoint, se lanza ApiNotConfiguredError: el formulario
 * muestra un error con los datos de contacto y nunca simula un envío correcto.
 */
(function () {
  'use strict';

  class ApiNotConfiguredError extends Error {
    constructor() {
      super('No hay un endpoint configurado para el formulario de contacto.');
      this.name = 'ApiNotConfiguredError';
    }
  }

  class ApiRequestError extends Error {
    constructor(status) {
      super(`La API respondió con el estado ${status}.`);
      this.name = 'ApiRequestError';
      this.status = status;
    }
  }

  async function submitContactRequest(data, { endpoint, timeoutMs = 15000 } = {}) {
    if (!endpoint) throw new ApiNotConfiguredError();

    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(data),
        signal: controller.signal,
      });
      if (!response.ok) throw new ApiRequestError(response.status);
      return await response.json().catch(() => ({}));
    } finally {
      window.clearTimeout(timer);
    }
  }

  window.AEOD = Object.assign(window.AEOD || {}, {
    api: { submitContactRequest, ApiNotConfiguredError, ApiRequestError },
  });
})();
