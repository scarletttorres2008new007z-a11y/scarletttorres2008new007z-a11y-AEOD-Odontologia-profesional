import { useMutation } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { erroresDeCampos, mensajeGeneral } from '../../shared/api/errores';
import { useTitulo } from '../../shared/hooks';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { CampoPassword } from '../../shared/ui/Campo';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import { cambiarMiPassword } from '../auth/api';
import { useAuth } from '../auth/contexto';
import css from './Cuenta.module.css';

export function PaginaMiCuenta() {
  useTitulo('Mi cuenta');
  const { usuario } = useAuth();
  if (!usuario) return null;

  return (
    <div className={p.pagina}>
      <div>
        <h1>Mi cuenta</h1>
        <p className={p.entradilla}>Tus datos de acceso al software.</p>
      </div>

      <div className={css.columnas}>
        <section className={p.tarjeta} aria-labelledby="titulo-mis-datos">
          <h2 id="titulo-mis-datos">Tus datos</h2>
          <dl className={css.datos}>
            <div>
              <dt>Nombre</dt>
              <dd>{usuario.nombre}</dd>
            </div>
            <div>
              <dt>Usuario</dt>
              <dd>{usuario.username}</dd>
            </div>
            <div>
              <dt>Correo</dt>
              <dd>{usuario.email}</dd>
            </div>
            <div>
              <dt>Roles</dt>
              <dd>
                {usuario.roles.length > 0 ? (
                  <span className={p.lista}>
                    {usuario.roles.map((rol) => (
                      <Insignia key={rol.codigo} tono="marca">
                        {rol.nombre}
                      </Insignia>
                    ))}
                  </span>
                ) : (
                  'Sin rol asignado'
                )}
              </dd>
            </div>
          </dl>
          <p className={p.secundario}>Si algún dato no es correcto, pide a un administrador que lo cambie.</p>
        </section>

        <CambioDePassword />
      </div>
    </div>
  );
}

function CambioDePassword() {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [repetida, setRepetida] = useState('');
  const [noCoinciden, setNoCoinciden] = useState(false);

  const cambiar = useMutation({
    mutationFn: () => cambiarMiPassword(actual, nueva),
    onSuccess: () => {
      setActual('');
      setNueva('');
      setRepetida('');
    },
  });

  const enviar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const distintas = nueva !== repetida;
    setNoCoinciden(distintas);
    if (distintas) {
      cambiar.reset();
      return;
    }
    cambiar.mutate();
  };

  const errores = erroresDeCampos(cambiar.error);
  const general = mensajeGeneral(cambiar.error, ['password_actual', 'password_nueva']);

  return (
    <section className={p.tarjeta} aria-labelledby="titulo-password">
      <form onSubmit={enviar} noValidate className={css.formulario}>
        <h2 id="titulo-password">Cambiar la contraseña</h2>
        <CampoPassword
          etiqueta="Contraseña actual"
          required
          autoComplete="current-password"
          value={actual}
          onChange={(e) => setActual(e.target.value)}
          error={errores.password_actual}
        />
        <CampoPassword
          etiqueta="Contraseña nueva"
          required
          autoComplete="new-password"
          value={nueva}
          onChange={(e) => setNueva(e.target.value)}
          ayuda="Mínimo 10 caracteres, distinta de tu usuario y de tu correo."
          error={errores.password_nueva}
        />
        <CampoPassword
          etiqueta="Repite la contraseña nueva"
          required
          autoComplete="new-password"
          value={repetida}
          onChange={(e) => setRepetida(e.target.value)}
          error={noCoinciden ? 'No coincide con la contraseña nueva.' : undefined}
        />
        {general && <Aviso tipo="error">{general}</Aviso>}
        {cambiar.isSuccess && (
          <Aviso tipo="exito">
            Contraseña cambiada. Si tenías la sesión abierta en otros equipos, allí se ha cerrado.
          </Aviso>
        )}
        <div>
          <Boton type="submit" cargando={cambiar.isPending}>
            Cambiar la contraseña
          </Boton>
        </div>
      </form>
    </section>
  );
}
