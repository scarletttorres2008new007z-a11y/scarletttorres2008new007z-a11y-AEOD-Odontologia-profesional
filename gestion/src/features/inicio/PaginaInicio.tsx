import { Link } from 'react-router';
import { seccionesVisibles } from '../../app/menu';
import { useTitulo } from '../../shared/hooks';
import { Vacio } from '../../shared/ui/Aviso';
import { Icono } from '../../shared/ui/Icono';
import p from '../../shared/ui/pagina.module.css';
import { useAuth } from '../auth/contexto';
import css from './Inicio.module.css';

/** Panel de inicio. Por ahora, accesos a los módulos que la persona puede usar. */
export function PaginaInicio() {
  useTitulo('Inicio');
  const { usuario, tienePermiso } = useAuth();
  const modulos = seccionesVisibles(tienePermiso).flatMap((seccion) => seccion.opciones);
  const nombre = usuario?.nombre.trim().split(/\s+/)[0] ?? '';

  return (
    <div className={p.pagina}>
      <div>
        <h1>Hola, {nombre}</h1>
        <p className={p.entradilla}>Te damos la bienvenida al software de gestión de AEOD.</p>
      </div>

      {modulos.length > 0 ? (
        <section aria-labelledby="titulo-modulos" className={css.modulos}>
          <h2 id="titulo-modulos" className="visually-hidden">
            Tus módulos
          </h2>
          <ul className={css.rejilla}>
            {modulos.map((modulo) => (
              <li key={modulo.ruta}>
                <Link to={modulo.ruta} className={css.modulo}>
                  <span className={css.icono}>
                    <Icono nombre={modulo.icono} tamano={22} />
                  </span>
                  <span className={css.moduloTexto}>
                    <span className={css.moduloTitulo}>{modulo.texto}</span>
                    <span className={css.moduloDescripcion}>{modulo.descripcion}</span>
                  </span>
                  <span className={css.flecha}>
                    <Icono nombre="flecha" tamano={18} />
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ) : (
        <Vacio>
          Tu usuario todavía no tiene acceso a ningún módulo. Si necesitas alguno para tu trabajo, pídeselo a
          un administrador.
        </Vacio>
      )}
    </div>
  );
}
