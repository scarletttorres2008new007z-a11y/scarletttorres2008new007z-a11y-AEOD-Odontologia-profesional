import { useRef, useState, type FormEvent } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import { useTitulo } from '../../shared/hooks';
import { Aviso } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Campo, CampoPassword } from '../../shared/ui/Campo';
import { Marca } from '../../shared/ui/Marca';
import css from '../../shared/ui/pantalla.module.css';
import { useAuth } from './contexto';

interface Faltan {
  usuario?: string;
  password?: string;
}

/** Entrada al software. Al entrar, la redirección la hace la ruta (vuelve a la página que se quería ver). */
export function PaginaEntrar() {
  useTitulo('Entrar');
  const { entrar, sesionTerminada } = useAuth();
  const [usuario, setUsuario] = useState('');
  const [password, setPassword] = useState('');
  const [faltan, setFaltan] = useState<Faltan>({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const campoUsuario = useRef<HTMLInputElement>(null);
  const campoPassword = useRef<HTMLInputElement>(null);

  const enviar = async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const pendientes: Faltan = {
      usuario: usuario.trim() ? undefined : 'Escribe tu usuario o tu correo.',
      password: password ? undefined : 'Escribe tu contraseña.',
    };
    setFaltan(pendientes);
    setError('');
    if (pendientes.usuario) return campoUsuario.current?.focus();
    if (pendientes.password) return campoPassword.current?.focus();

    setEnviando(true);
    try {
      await entrar(usuario.trim(), password);
    } catch (e) {
      setError(mensajeDeError(e));
      setEnviando(false);
    }
  };

  return (
    <main className={css.pantalla}>
      <div className={css.caja}>
        <Marca />
        <div>
          <h1>Software de gestión</h1>
          <p className={css.texto}>Acceso para el personal de la clínica.</p>
        </div>

        {sesionTerminada && !error && (
          <Aviso tipo="alerta">Tu sesión ha terminado. Vuelve a entrar para continuar.</Aviso>
        )}

        <form onSubmit={enviar} noValidate className={css.formulario}>
          <Campo
            ref={campoUsuario}
            etiqueta="Usuario o correo"
            name="usuario"
            autoComplete="username"
            autoCapitalize="off"
            spellCheck={false}
            autoFocus
            required
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            error={faltan.usuario}
          />
          <CampoPassword
            ref={campoPassword}
            etiqueta="Contraseña"
            name="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={faltan.password}
          />
          {error && <Aviso tipo="error">{error}</Aviso>}
          <Boton type="submit" cargando={enviando}>
            Entrar
          </Boton>
        </form>

        <p className={css.pie}>
          ¿Has olvidado la contraseña? Pide a un administrador de la clínica que te la restablezca.
        </p>
      </div>
    </main>
  );
}
