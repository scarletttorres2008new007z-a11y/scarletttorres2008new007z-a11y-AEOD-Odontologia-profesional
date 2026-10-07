import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { mensajeDeError } from '../../shared/api/errores';
import type { Permiso, Rol } from '../../shared/api/tipos';
import { useTitulo } from '../../shared/hooks';
import { Aviso, Cargando } from '../../shared/ui/Aviso';
import { Boton } from '../../shared/ui/Boton';
import { Casilla } from '../../shared/ui/Campo';
import { Insignia } from '../../shared/ui/Insignia';
import p from '../../shared/ui/pagina.module.css';
import ui from '../../shared/ui/ui.module.css';
import { useAuth } from '../auth/contexto';
import { PERMISOS } from '../auth/permisos';
import { CLAVE_PERMISOS, CLAVE_ROLES, cambiarPermisosDelRol, listarPermisos, listarRoles } from './api';
import css from './Roles.module.css';

interface Modulo {
  nombre: string;
  permisos: Permiso[];
}

export function PaginaRoles() {
  useTitulo('Roles y permisos');
  const { tienePermiso } = useAuth();
  const roles = useQuery({ queryKey: CLAVE_ROLES, queryFn: listarRoles });
  const permisos = useQuery({ queryKey: CLAVE_PERMISOS, queryFn: listarPermisos, staleTime: Infinity });
  const puedeEditar = tienePermiso(PERMISOS.ROLES_EDITAR);
  const error = roles.error ?? permisos.error;

  return (
    <div className={p.pagina}>
      <div>
        <h1>Roles y permisos</h1>
        <p className={p.entradilla}>
          Qué puede hacer cada rol. Un cambio se aplica al momento a todas las personas que tienen ese rol.
        </p>
      </div>

      {error && <Aviso tipo="error">{mensajeDeError(error)}</Aviso>}
      {(roles.isPending || permisos.isPending) && !error && <Cargando texto="Cargando roles…" />}
      {roles.data && permisos.data && (
        <div className={css.roles}>
          {roles.data.map((rol) => (
            <TarjetaRol key={rol.id} rol={rol} modulos={agrupar(permisos.data)} puedeEditar={puedeEditar} />
          ))}
        </div>
      )}
    </div>
  );
}

function TarjetaRol({ rol, modulos, puedeEditar }: { rol: Rol; modulos: Modulo[]; puedeEditar: boolean }) {
  const queryClient = useQueryClient();
  const idTitulo = useId();
  // null = sin cambios: se ven los permisos que tiene guardados
  const [cambios, setCambios] = useState<string[] | null>(null);
  const [guardado, setGuardado] = useState(false);
  const elegidos = cambios ?? rol.permisos;
  const hayCambios = cambios !== null && !mismosCodigos(cambios, rol.permisos);
  const editable = rol.editable && puedeEditar;

  const guardar = useMutation({
    mutationFn: () => cambiarPermisosDelRol(rol.id, elegidos),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: CLAVE_ROLES }),
        // Por si la persona tiene este rol: su menú cambia también
        queryClient.invalidateQueries({ queryKey: ['usuario-actual'] }),
      ]);
      setCambios(null);
      setGuardado(true);
    },
  });

  const marcar = (codigo: string, marcado: boolean) => {
    setGuardado(false);
    guardar.reset();
    setCambios(marcado ? [...elegidos, codigo] : elegidos.filter((c) => c !== codigo));
  };

  return (
    <section className={`${p.tarjeta} ${css.rol}`} aria-labelledby={idTitulo}>
      <div className={css.cabecera}>
        <div>
          <h2 id={idTitulo}>{rol.nombre}</h2>
          {rol.descripcion && <p className={p.entradilla}>{rol.descripcion}</p>}
        </div>
        <Insignia tono="marca">
          {rol.usuarios} {rol.usuarios === 1 ? 'usuario' : 'usuarios'}
        </Insignia>
      </div>

      <div className={css.modulos}>
        {modulos.map((modulo) => (
          <fieldset key={modulo.nombre} className={ui.grupo}>
            <legend>{modulo.nombre}</legend>
            {modulo.permisos.map((permiso) => (
              <Casilla
                key={permiso.codigo}
                etiqueta={permiso.descripcion}
                checked={elegidos.includes(permiso.codigo)}
                disabled={!editable}
                onChange={(e) => marcar(permiso.codigo, e.target.checked)}
              />
            ))}
          </fieldset>
        ))}
      </div>

      {!rol.editable && <p className={p.secundario}>El administrador tiene siempre todos los permisos.</p>}
      {editable && (
        <div className={css.pie}>
          {guardar.error && <Aviso tipo="error">{mensajeDeError(guardar.error)}</Aviso>}
          {guardado && (
            <p role="status" className={css.guardado}>
              Cambios guardados.
            </p>
          )}
          <div className={p.acciones}>
            <Boton
              variante="secundario"
              disabled={!hayCambios || guardar.isPending}
              onClick={() => setCambios(null)}
            >
              Descartar
            </Boton>
            <Boton cargando={guardar.isPending} disabled={!hayCambios} onClick={() => guardar.mutate()}>
              Guardar cambios
            </Boton>
          </div>
        </div>
      )}
    </section>
  );
}

/** Permisos agrupados por módulo, en el orden en que llegan de la API. */
function agrupar(permisos: Permiso[]): Modulo[] {
  const modulos: Modulo[] = [];
  for (const permiso of permisos) {
    let modulo = modulos.find((m) => m.nombre === permiso.modulo);
    if (!modulo) {
      modulo = { nombre: permiso.modulo, permisos: [] };
      modulos.push(modulo);
    }
    modulo.permisos.push(permiso);
  }
  return modulos;
}

function mismosCodigos(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((codigo) => b.includes(codigo));
}
