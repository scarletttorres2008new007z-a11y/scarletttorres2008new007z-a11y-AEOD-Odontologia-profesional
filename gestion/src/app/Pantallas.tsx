import { useTitulo } from '../shared/hooks';
import { Cargando } from '../shared/ui/Aviso';
import { Boton } from '../shared/ui/Boton';
import { Marca } from '../shared/ui/Marca';
import css from '../shared/ui/pantalla.module.css';

/** Mientras se comprueba si ya había una sesión abierta. */
export function PantallaCargando() {
  return (
    <main className={css.pantalla}>
      <Cargando texto="Abriendo el software…" />
    </main>
  );
}

/** El backend no responde: sin él no se puede hacer nada (el software nunca va directo a la base de datos). */
export function PantallaSinConexion({ alReintentar }: { alReintentar: () => void }) {
  useTitulo('Sin conexión');
  return (
    <main className={css.pantalla}>
      <div className={css.caja}>
        <Marca />
        <h1>No se puede conectar con el servidor</h1>
        <p className={css.texto}>
          El software necesita la API de la clínica (el backend) para funcionar. Comprueba que está arrancado
          y vuelve a intentarlo.
        </p>
        <Boton onClick={alReintentar}>Reintentar</Boton>
      </div>
    </main>
  );
}
