import css from './pantalla.module.css';

export function Marca() {
  return (
    <p className={css.marca}>
      <span className={css.marcaNombre}>AEOD</span>
      <span className={css.marcaDetalle}>Odontología profesional</span>
    </p>
  );
}
