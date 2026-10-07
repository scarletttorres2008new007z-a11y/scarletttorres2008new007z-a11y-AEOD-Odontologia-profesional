import { Boton } from './Boton';
import css from './ui.module.css';

interface Props {
  /** Empieza en 0, como en la API. */
  pagina: number;
  totalPaginas: number;
  totalElementos: number;
  alCambiar: (pagina: number) => void;
}

export function Paginacion({ pagina, totalPaginas, totalElementos, alCambiar }: Props) {
  if (totalPaginas <= 1) {
    return (
      <p className={css.paginacion}>
        {totalElementos} {totalElementos === 1 ? 'resultado' : 'resultados'}
      </p>
    );
  }
  return (
    <nav className={css.paginacion} aria-label="Páginas">
      <span>
        Página {pagina + 1} de {totalPaginas} · {totalElementos} resultados
      </span>
      <span className={css.paginacionBotones}>
        <Boton variante="secundario" pequeno disabled={pagina === 0} onClick={() => alCambiar(pagina - 1)}>
          Anterior
        </Boton>
        <Boton
          variante="secundario"
          pequeno
          disabled={pagina + 1 >= totalPaginas}
          onClick={() => alCambiar(pagina + 1)}
        >
          Siguiente
        </Boton>
      </span>
    </nav>
  );
}
