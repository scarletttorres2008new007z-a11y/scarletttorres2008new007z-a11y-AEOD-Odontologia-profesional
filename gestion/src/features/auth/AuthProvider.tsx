import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { avisarAlTerminarSesion, renovarSesion } from '../../shared/api/cliente';
import type { UsuarioActual } from '../../shared/api/tipos';
import { cerrarSesion, iniciarSesion, obtenerUsuarioActual } from './api';
import { AuthContexto, type Auth, type EstadoSesion } from './contexto';
import type { CodigoPermiso } from './permisos';

/**
 * Sesión del software. Al abrir la página intenta recuperar la sesión con la cookie (sin volver a pedir la
 * contraseña). Mientras hay sesión, los datos y permisos del usuario se vuelven a pedir a la API de vez en
 * cuando, así un cambio de permisos se refleja también en los menús.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [estado, setEstado] = useState<EstadoSesion>('comprobando');
  const [usuarioAlEntrar, setUsuarioAlEntrar] = useState<UsuarioActual | null>(null);
  const [sesionTerminada, setSesionTerminada] = useState(false);

  // Mientras se está comprobando (al abrir, o al reintentar si el servidor no respondía), se intenta
  // recuperar la sesión con la cookie
  const comprobando = estado === 'comprobando';
  useEffect(() => {
    if (!comprobando) return;
    let vigente = true;
    renovarSesion().then(
      (sesion) => {
        if (!vigente) return;
        setUsuarioAlEntrar(sesion?.usuario ?? null);
        setEstado(sesion ? 'con-sesion' : 'sin-sesion');
      },
      () => {
        if (vigente) setEstado('sin-conexion');
      },
    );
    return () => {
      vigente = false;
    };
  }, [comprobando]);

  // Si una petición descubre que la sesión ya no vale, se vuelve a la pantalla de entrada
  useEffect(() => {
    avisarAlTerminarSesion(() => {
      queryClient.clear();
      setUsuarioAlEntrar(null);
      setSesionTerminada(true);
      setEstado('sin-sesion');
    });
    return () => avisarAlTerminarSesion(null);
  }, [queryClient]);

  const { data: usuarioActualizado } = useQuery({
    queryKey: ['usuario-actual'],
    queryFn: obtenerUsuarioActual,
    enabled: estado === 'con-sesion',
    staleTime: 60_000,
    refetchInterval: 5 * 60_000,
  });

  const usuario = estado === 'con-sesion' ? (usuarioActualizado ?? usuarioAlEntrar) : null;

  const entrar = useCallback(
    async (identificador: string, password: string) => {
      const sesion = await iniciarSesion(identificador, password);
      queryClient.clear();
      queryClient.setQueryData(['usuario-actual'], sesion.usuario);
      setUsuarioAlEntrar(sesion.usuario);
      setSesionTerminada(false);
      setEstado('con-sesion');
    },
    [queryClient],
  );

  // Si el servidor no confirma el cierre, la sesión sigue abierta allí: se avisa en vez de fingir que se cerró
  const salir = useCallback(async () => {
    await cerrarSesion();
    queryClient.clear();
    setUsuarioAlEntrar(null);
    setSesionTerminada(false);
    setEstado('sin-sesion');
  }, [queryClient]);

  const reintentar = useCallback(() => setEstado('comprobando'), []);

  const valor = useMemo<Auth>(
    () => ({
      estado,
      usuario,
      sesionTerminada,
      entrar,
      salir,
      reintentar,
      tienePermiso: (permiso: CodigoPermiso) => usuario?.permisos.includes(permiso) ?? false,
    }),
    [estado, usuario, sesionTerminada, entrar, salir, reintentar],
  );

  return <AuthContexto.Provider value={valor}>{children}</AuthContexto.Provider>;
}
