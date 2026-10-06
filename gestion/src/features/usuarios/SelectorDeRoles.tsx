import { useId } from 'react';
import type { Rol } from '../../shared/api/tipos';
import { Casilla } from '../../shared/ui/Campo';
import ui from '../../shared/ui/ui.module.css';

interface SelectorDeRolesProps {
  roles: Rol[];
  elegidos: string[];
  alCambiar: (codigos: string[]) => void;
  bloqueados?: Partial<Record<string, string>>;
  error?: string;
}

export function SelectorDeRoles({
  roles,
  elegidos,
  alCambiar,
  bloqueados = {},
  error,
}: SelectorDeRolesProps) {
  const idError = useId();
  return (
    <fieldset className={ui.grupo} aria-describedby={error ? idError : undefined}>
      <legend>Roles</legend>
      {roles.map((rol) => {
        const motivo = bloqueados[rol.codigo];
        return (
          <Casilla
            key={rol.codigo}
            etiqueta={rol.nombre}
            detalle={motivo ?? rol.descripcion}
            disabled={motivo !== undefined}
            checked={elegidos.includes(rol.codigo)}
            onChange={(evento) =>
              alCambiar(
                evento.target.checked
                  ? [...elegidos, rol.codigo]
                  : elegidos.filter((codigo) => codigo !== rol.codigo),
              )
            }
          />
        );
      })}
      {error && (
        <p id={idError} className={ui.error}>
          {error}
        </p>
      )}
    </fieldset>
  );
}
