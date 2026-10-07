import { useEffect, useId, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router';
import { useAuth } from '../features/auth/contexto';
import { mensajeDeError } from '../shared/api/errores';
import { Aviso } from '../shared/ui/Aviso';
import { Icono } from '../shared/ui/Icono';
import css from './Estructura.module.css';
import { INICIO, seccionesVisibles, type OpcionMenu } from './menu';

/** Estructura de todas las páginas con sesión: cabecera, menú lateral (desplegable en el móvil) y contenido. */
export function Estructura() {
  const { usuario, salir, tienePermiso } = useAuth();
  const { pathname } = useLocation();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [saliendo, setSaliendo] = useState(false);
  const [errorAlSalir, setErrorAlSalir] = useState('');
  const principal = useRef<HTMLElement>(null);
  const botonMenu = useRef<HTMLButtonElement>(null);
  const rutaAnterior = useRef(pathname);
  const idMenu = useId();

  // Al cambiar de página, el foco va al contenido nuevo (así el lector de pantalla empieza por su título)
  useEffect(() => {
    if (rutaAnterior.current === pathname) return;
    rutaAnterior.current = pathname;
    principal.current?.focus();
  }, [pathname]);

  // En el móvil, Escape cierra el menú y devuelve el foco al botón que lo abrió
  useEffect(() => {
    if (!menuAbierto) return;
    const alPulsar = (evento: KeyboardEvent) => {
      if (evento.key !== 'Escape') return;
      setMenuAbierto(false);
      botonMenu.current?.focus();
    };
    document.addEventListener('keydown', alPulsar);
    return () => document.removeEventListener('keydown', alPulsar);
  }, [menuAbierto]);

  if (!usuario) return null;

  const cerrarSesion = async () => {
    setSaliendo(true);
    setErrorAlSalir('');
    try {
      await salir();
    } catch (error) {
      setErrorAlSalir(`No se ha podido cerrar la sesión. ${mensajeDeError(error)}`);
      setSaliendo(false);
    }
  };

  const cerrarMenu = () => setMenuAbierto(false);
  const roles = usuario.roles.map((rol) => rol.nombre).join(', ');

  return (
    <div className={css.estructura}>
      <a href="#contenido" className={css.saltar}>
        Saltar al contenido
      </a>

      <header className={css.cabecera}>
        <button
          ref={botonMenu}
          type="button"
          className={css.botonMenu}
          aria-expanded={menuAbierto}
          aria-controls={idMenu}
          onClick={() => setMenuAbierto((abierto) => !abierto)}
        >
          <Icono nombre={menuAbierto ? 'cerrar' : 'menu'} />
          <span className="visually-hidden">Menú</span>
        </button>

        <Link to="/" className={css.marca} onClick={cerrarMenu}>
          <span className={css.marcaNombre}>AEOD</span>
          <span className={css.marcaDetalle}>Gestión</span>
        </Link>

        <div className={css.usuario}>
          <Link to="/mi-cuenta" className={css.cuenta} onClick={cerrarMenu}>
            <span className={css.avatar} aria-hidden="true">
              {iniciales(usuario.nombre)}
            </span>
            <span className="visually-hidden">Mi cuenta:</span>{' '}
            <span className={css.cuentaTexto}>
              <span className={css.cuentaNombre}>{usuario.nombre}</span>{' '}
              <span className={css.cuentaRoles}>{roles || 'Sin rol asignado'}</span>
            </span>
          </Link>
          <button type="button" className={css.salir} onClick={cerrarSesion} disabled={saliendo}>
            <Icono nombre="salir" tamano={18} />
            <span className={css.salirTexto}>Cerrar sesión</span>
          </button>
        </div>
      </header>

      <div className={css.cuerpo}>
        {menuAbierto && <div className={css.fondo} onClick={cerrarMenu} aria-hidden="true" />}

        <nav id={idMenu} className={css.lateral} data-abierto={menuAbierto} aria-label="Menú principal">
          <ul className={css.menu}>
            <li>
              <Opcion opcion={INICIO} alElegir={cerrarMenu} />
            </li>
          </ul>
          {seccionesVisibles(tienePermiso).map((seccion) => (
            <div key={seccion.titulo} className={css.seccion}>
              <p className={css.seccionTitulo} aria-hidden="true">
                {seccion.titulo}
              </p>
              <ul className={css.menu} aria-label={seccion.titulo}>
                {seccion.opciones.map((opcion) => (
                  <li key={opcion.ruta}>
                    <Opcion opcion={opcion} alElegir={cerrarMenu} />
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>

        <main id="contenido" ref={principal} tabIndex={-1} className={css.contenido}>
          {errorAlSalir && (
            <div className={css.avisoGeneral}>
              <Aviso tipo="error">{errorAlSalir}</Aviso>
            </div>
          )}
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function Opcion({ opcion, alElegir }: { opcion: OpcionMenu; alElegir: () => void }) {
  return (
    <NavLink to={opcion.ruta} end={opcion.ruta === '/'} className={css.opcion} onClick={alElegir}>
      <Icono nombre={opcion.icono} />
      {opcion.texto}
    </NavLink>
  );
}

function iniciales(nombre: string): string {
  const partes = nombre.trim().split(/\s+/);
  const primera = partes[0]?.[0] ?? '';
  const ultima = partes.length > 1 ? (partes.at(-1)?.[0] ?? '') : '';
  return (primera + ultima).toUpperCase();
}
