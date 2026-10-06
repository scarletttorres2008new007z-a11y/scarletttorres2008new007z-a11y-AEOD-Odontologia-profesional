import { Link } from 'react-router';
import { useTitulo } from '../shared/hooks';
import p from '../shared/ui/pagina.module.css';

export function PaginaNoEncontrada() {
  useTitulo('Página no encontrada');
  return (
    <div className={p.pagina}>
      <div>
        <h1>Esta página no existe</h1>
        <p className={p.entradilla}>Puede que el enlace esté mal escrito o que la página se haya movido.</p>
      </div>
      <p>
        <Link to="/">Volver al inicio</Link>
      </p>
    </div>
  );
}

export function PaginaSinPermiso() {
  useTitulo('Sin permiso');
  return (
    <div className={p.pagina}>
      <div>
        <h1>No tienes permiso para ver esta página</h1>
        <p className={p.entradilla}>
          Tu usuario no tiene el permiso necesario. Si lo necesitas para tu trabajo, pídeselo a un
          administrador.
        </p>
      </div>
      <p>
        <Link to="/">Volver al inicio</Link>
      </p>
    </div>
  );
}
