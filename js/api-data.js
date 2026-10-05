/**
 * Datos dinámicos desde la API: precios de tratamientos y equipo.
 *
 * - Si la API responde, la lista de precios y el equipo se actualizan con los datos de la base de datos.
 *   Los elementos se emparejan por nombre para conservar lo que la base de datos no guarda
 *   (categoría del filtro, "por pieza", credenciales del equipo).
 * - Si la API no está configurada, no responde o devuelve una lista vacía,
 *   la página se queda con su contenido estático. Nunca se vacía una sección.
 *
 * Expone AEOD.catalogo → Promise<{ tratamientos, odontologos }> para la reserva online
 * (listas vacías si la API no está configurada o no responde).
 */
(function () {
  'use strict';

  const AEOD = window.AEOD || {};
  const { api, formatEuro } = AEOD;
  if (!api) return;

  const normalize = (text) => String(text || '').trim().toLocaleLowerCase('es')
    .normalize('NFD').replace(/[̀-ͯ]/g, '');
  const isSafeImage = (url) => typeof url === 'string' && /^(https?:\/\/|[\w.\/-]+$)/i.test(url);
  const imagePath = (url) => String(url || '').split('?')[0];

  /* ── Tratamientos y precios ── */
  const priceList = document.querySelector('[data-price-list]');

  function priceText(precio, perUnit) {
    if (!(precio > 0)) return 'Gratuita';
    return `desde ${formatEuro(precio)}${perUnit ? ' / pieza' : ''}`;
  }

  function createPriceItem(tratamiento) {
    const li = document.createElement('li');
    li.className = 'price-item';
    li.dataset.id = `api-${tratamiento.id}`;
    li.dataset.category = '';
    li.dataset.form = '';
    const name = document.createElement('span');
    name.className = 'price-item__name';
    name.textContent = tratamiento.nombre;
    const price = document.createElement('span');
    price.className = 'price-item__price';
    li.append(name, price);
    return li;
  }

  function renderPrices(tratamientos) {
    if (!priceList || !tratamientos.length) return;
    const existing = new Map(Array.from(priceList.querySelectorAll('.price-item'))
      .map((li) => [normalize(li.querySelector('.price-item__name').textContent), li]));

    const items = tratamientos.map((tratamiento) => {
      const li = existing.get(normalize(tratamiento.nombre)) || createPriceItem(tratamiento);
      const precio = Number(tratamiento.precio_desde) || 0;
      li.dataset.price = String(precio);
      li.dataset.tratamientoId = String(tratamiento.id);
      li.querySelector('.price-item__price').textContent = priceText(precio, li.hasAttribute('data-per-unit'));
      if (tratamiento.descripcion_corta) li.title = tratamiento.descripcion_corta;
      return li;
    });
    // Los tratamientos inactivos (que ya no devuelve la API) desaparecen de la lista
    priceList.replaceChildren(...items);
    document.dispatchEvent(new CustomEvent('aeod:precios-actualizados'));
  }

  /* ── Equipo ── */
  const teamGrid = document.querySelector('.team__grid');

  function createTeamCard() {
    const article = document.createElement('article');
    article.className = 'team-card';
    const img = document.createElement('img');
    img.className = 'team-card__photo';
    img.width = 600;
    img.height = 800;
    img.alt = '';
    img.loading = 'lazy';
    img.decoding = 'async';
    const body = document.createElement('div');
    body.className = 'team-card__body';
    body.innerHTML = '<h3 class="team-card__name"></h3><p class="team-card__role"></p><p class="team-card__bio"></p>';
    article.append(img, body);
    return article;
  }

  function renderTeam(odontologos) {
    if (!teamGrid || !odontologos.length) return;
    const existing = new Map(Array.from(teamGrid.querySelectorAll('.team-card'))
      .map((card) => [normalize(card.querySelector('.team-card__name').textContent), card]));

    const cards = odontologos.map((odontologo) => {
      const card = existing.get(normalize(odontologo.nombre)) || createTeamCard();
      card.querySelector('.team-card__name').textContent = odontologo.nombre;
      card.querySelector('.team-card__role').textContent = odontologo.especialidad || '';
      card.querySelector('.team-card__bio').textContent = odontologo.descripcion || '';
      const img = card.querySelector('.team-card__photo');
      if (!img.dataset.fallback && isSafeImage(odontologo.imagen) && imagePath(odontologo.imagen) !== imagePath(img.getAttribute('src'))) {
        img.removeAttribute('srcset');
        img.removeAttribute('sizes');
        img.src = odontologo.imagen;
      }
      return card;
    });
    teamGrid.replaceChildren(...cards);
  }

  /* ── Carga ── */
  const lista = (promesa) => promesa.then((datos) => (Array.isArray(datos) ? datos : [])).catch(() => []);
  const tratamientosCargados = api.enabled ? lista(api.getTratamientos()) : Promise.resolve([]);
  const odontologosCargados = api.enabled ? lista(api.getOdontologos()) : Promise.resolve([]);

  tratamientosCargados.then(renderPrices);
  // Sin API se mantiene el equipo estático
  odontologosCargados.then(renderTeam);

  AEOD.catalogo = Promise.all([tratamientosCargados, odontologosCargados])
    .then(([tratamientos, odontologos]) => ({ tratamientos, odontologos }));
})();
