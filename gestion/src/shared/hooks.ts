import { useEffect, useState } from 'react';

/** Título de la pestaña del navegador: «Usuarios · AEOD Gestión». */
export function useTitulo(titulo: string): void {
  useEffect(() => {
    document.title = `${titulo} · AEOD Gestión`;
  }, [titulo]);
}

/** Devuelve el valor cuando lleva un momento sin cambiar, para no llamar a la API con cada tecla. */
export function useValorEstable<T>(valor: T, milisegundos = 300): T {
  const [estable, setEstable] = useState(valor);
  useEffect(() => {
    const espera = setTimeout(() => setEstable(valor), milisegundos);
    return () => clearTimeout(espera);
  }, [valor, milisegundos]);
  return estable;
}
