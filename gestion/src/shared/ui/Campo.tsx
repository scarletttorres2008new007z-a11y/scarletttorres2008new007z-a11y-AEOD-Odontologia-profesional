import {
  useId,
  useState,
  type InputHTMLAttributes,
  type ReactNode,
  type Ref,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';
import css from './ui.module.css';
import { TextoOculto } from './TextoOculto';

interface Comunes {
  etiqueta: string;
  error?: string;
  ayuda?: ReactNode;
}

interface EnvoltorioProps extends Comunes {
  idCampo: string;
  obligatorio?: boolean;
  children: ReactNode;
}

/** Etiqueta, ayuda y error alrededor de un control. Los ids enlazan todo para los lectores de pantalla. */
function Envoltorio({ etiqueta, error, ayuda, idCampo, obligatorio, children }: EnvoltorioProps) {
  return (
    <div className={css.campo}>
      <label htmlFor={idCampo} className={css.etiqueta}>
        {etiqueta}
        {obligatorio && (
          <span className={css.obligatorio} aria-hidden="true">
            {' '}
            *
          </span>
        )}
      </label>
      {children}
      {ayuda && (
        <p id={`${idCampo}-ayuda`} className={css.ayuda}>
          {ayuda}
        </p>
      )}
      {error && (
        <p id={`${idCampo}-error`} className={css.error}>
          {error}
        </p>
      )}
    </div>
  );
}

function descritoPor(idCampo: string, { ayuda, error }: Comunes): string | undefined {
  const ids = [ayuda ? `${idCampo}-ayuda` : '', error ? `${idCampo}-error` : ''].join(' ').trim();
  return ids || undefined;
}

type InputProps = Comunes & InputHTMLAttributes<HTMLInputElement> & { ref?: Ref<HTMLInputElement> };

/** Texto, correo, número o fecha, con su etiqueta, ayuda y error. */
export function Campo({ etiqueta, error, ayuda, required, id, ...resto }: InputProps) {
  const generado = useId();
  const idCampo = id ?? generado;
  return (
    <Envoltorio etiqueta={etiqueta} error={error} ayuda={ayuda} idCampo={idCampo} obligatorio={required}>
      <input
        id={idCampo}
        className={css.control}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={descritoPor(idCampo, { etiqueta, error, ayuda })}
        {...resto}
      />
    </Envoltorio>
  );
}

/** Contraseña con un botón para mostrarla mientras se escribe. */
export function CampoPassword({ etiqueta, error, ayuda, required, id, ...resto }: Omit<InputProps, 'type'>) {
  const generado = useId();
  const idCampo = id ?? generado;
  const [visible, setVisible] = useState(false);
  return (
    <Envoltorio etiqueta={etiqueta} error={error} ayuda={ayuda} idCampo={idCampo} obligatorio={required}>
      <div className={css.conBoton}>
        <input
          id={idCampo}
          type={visible ? 'text' : 'password'}
          className={css.control}
          required={required}
          spellCheck={false}
          autoCapitalize="off"
          autoCorrect="off"
          aria-invalid={error ? true : undefined}
          aria-describedby={descritoPor(idCampo, { etiqueta, error, ayuda })}
          {...resto}
        />
        <button
          type="button"
          className={css.botonDentro}
          aria-controls={idCampo}
          onClick={() => setVisible((antes) => !antes)}
        >
          {visible ? 'Ocultar' : 'Mostrar'}
          <TextoOculto>la contraseña</TextoOculto>
        </button>
      </div>
    </Envoltorio>
  );
}

type SelectProps = Comunes & SelectHTMLAttributes<HTMLSelectElement>;

export function Selector({ etiqueta, error, ayuda, id, children, ...resto }: SelectProps) {
  const generado = useId();
  const idCampo = id ?? generado;
  return (
    <Envoltorio etiqueta={etiqueta} error={error} ayuda={ayuda} idCampo={idCampo}>
      <select
        id={idCampo}
        className={css.control}
        aria-invalid={error ? true : undefined}
        aria-describedby={descritoPor(idCampo, { etiqueta, error, ayuda })}
        {...resto}
      >
        {children}
      </select>
    </Envoltorio>
  );
}

type TextareaProps = Comunes & TextareaHTMLAttributes<HTMLTextAreaElement>;

/** Texto largo en varias líneas. */
export function AreaDeTexto({ etiqueta, error, ayuda, required, id, ...resto }: TextareaProps) {
  const generado = useId();
  const idCampo = id ?? generado;
  return (
    <Envoltorio etiqueta={etiqueta} error={error} ayuda={ayuda} idCampo={idCampo} obligatorio={required}>
      <textarea
        id={idCampo}
        className={css.control}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={descritoPor(idCampo, { etiqueta, error, ayuda })}
        {...resto}
      />
    </Envoltorio>
  );
}

interface CasillaProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  etiqueta: ReactNode;
  detalle?: ReactNode;
}

export function Casilla({ etiqueta, detalle, disabled, ...resto }: CasillaProps) {
  return (
    <label className={css.casilla} data-desactivada={disabled ? 'true' : undefined}>
      <input type="checkbox" disabled={disabled} {...resto} />
      <span>
        {etiqueta}
        {detalle && <span className={css.casillaDetalle}>{detalle}</span>}
      </span>
    </label>
  );
}
