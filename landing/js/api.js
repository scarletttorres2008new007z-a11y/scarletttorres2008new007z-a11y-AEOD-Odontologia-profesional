/**
 * Capa de acceso al backend (API REST de la clínica). Punto único para todas las llamadas.
 *
 * La URL base se lee de <meta name="aeod-api" content="http://localhost:8080/api"> en index.html.
 * Si el meta está vacío, la API se considera desactivada: la landing usa sus datos estáticos
 * y el formulario muestra el teléfono en lugar de simular un envío.
 *
 *   AEOD.api.getHealth()            GET  /health
 *   AEOD.api.getTratamientos()      GET  /tratamientos
 *   AEOD.api.getTratamiento(id)     GET  /tratamientos/{id}
 *   AEOD.api.getOdontologos()       GET  /odontologos
 *   AEOD.api.getOdontologo(id)      GET  /odontologos/{id}
 *   AEOD.api.getDisponibilidad(q)   GET  /disponibilidad?tratamiento_id&fecha[&odontologo_id][&franja]
 *   AEOD.api.getProximosHorarios(q) GET  /disponibilidad/proximos?tratamiento_id[&desde][&odontologo_id][&franja]
 *   AEOD.api.enviarContacto(datos)  POST /contacto
 *   AEOD.api.reservarCita(datos)    POST /citas  (409 si el horario ya no está libre)
 *   AEOD.api.cancelarCita(codigo)   POST /citas/cancelacion  (409 si ya no se puede cancelar online)
 */
(function () {
  'use strict';

  const meta = document.querySelector('meta[name="aeod-api"]');
  const BASE_URL = ((meta && meta.content) || '').trim().replace(/\/+$/, '');

  class ApiNotConfiguredError extends Error {
    constructor() {
      super('No hay una URL de API configurada.');
      this.name = 'ApiNotConfiguredError';
    }
  }

  /** Respuesta no 2xx. `errores` trae un mensaje por campo cuando la API lo envía (400). */
  class ApiRequestError extends Error {
    constructor(status, body) {
      super((body && body.message) || `La API respondió con el estado ${status}.`);
      this.name = 'ApiRequestError';
      this.status = status;
      this.errores = (body && body.errores) || {};
    }
  }

  async function request(path, { method = 'GET', body, timeoutMs = 15000 } = {}) {
    if (!BASE_URL) throw new ApiNotConfiguredError();

    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(BASE_URL + path, {
        method,
        headers: body ? { 'Content-Type': 'application/json', Accept: 'application/json' } : { Accept: 'application/json' },
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      });
      const data = await response.json().catch(() => null);
      if (!response.ok) throw new ApiRequestError(response.status, data);
      return data;
    } finally {
      window.clearTimeout(timer);
    }
  }

  /** { tratamiento_id: 2, fecha: '2026-10-15', franja: '' } → "?tratamiento_id=2&fecha=2026-10-15" (omite vacíos) */
  function query(params) {
    const search = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') search.set(key, String(value));
    });
    const text = search.toString();
    return text ? `?${text}` : '';
  }

  // Las lecturas usan un tiempo corto: si la API no responde, la página sigue con sus datos estáticos.
  const READ_TIMEOUT = 5000;

  window.AEOD = Object.assign(window.AEOD || {}, {
    api: {
      enabled: Boolean(BASE_URL),
      getHealth: () => request('/health', { timeoutMs: READ_TIMEOUT }),
      getTratamientos: () => request('/tratamientos', { timeoutMs: READ_TIMEOUT }),
      getTratamiento: (id) => request(`/tratamientos/${encodeURIComponent(id)}`, { timeoutMs: READ_TIMEOUT }),
      getOdontologos: () => request('/odontologos', { timeoutMs: READ_TIMEOUT }),
      getOdontologo: (id) => request(`/odontologos/${encodeURIComponent(id)}`, { timeoutMs: READ_TIMEOUT }),
      getDisponibilidad: (params) => request(`/disponibilidad${query(params)}`, { timeoutMs: READ_TIMEOUT * 2 }),
      getProximosHorarios: (params) => request(`/disponibilidad/proximos${query(params)}`, { timeoutMs: READ_TIMEOUT * 2 }),
      enviarContacto: (datos) => request('/contacto', { method: 'POST', body: datos }),
      reservarCita: (datos) => request('/citas', { method: 'POST', body: datos }),
      cancelarCita: (codigo) => request('/citas/cancelacion', { method: 'POST', body: { codigo } }),
      ApiNotConfiguredError,
      ApiRequestError,
    },
  });
})();
