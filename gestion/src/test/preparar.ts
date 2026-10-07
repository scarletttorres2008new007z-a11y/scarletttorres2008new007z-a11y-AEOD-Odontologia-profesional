// Se ejecuta antes de cada archivo de pruebas.
import '@testing-library/jest-dom/vitest';

// jsdom no abre <dialog> como el navegador: basta con marcarlo abierto o cerrado
function abrir(this: HTMLDialogElement) {
  this.open = true;
}

function cerrar(this: HTMLDialogElement) {
  this.open = false;
}

if (typeof HTMLDialogElement !== 'undefined' && !HTMLDialogElement.prototype.showModal) {
  HTMLDialogElement.prototype.showModal = abrir;
  HTMLDialogElement.prototype.close = cerrar;
}
