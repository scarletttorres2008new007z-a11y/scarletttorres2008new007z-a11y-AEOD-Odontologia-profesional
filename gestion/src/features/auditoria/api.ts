import { api } from '../../shared/api/cliente';
import type { AccionAuditoria, EntidadAuditoria, PaginaAuditoria } from '../../shared/api/tipos';

export const TAMANO_PAGINA = 25;

export interface FiltroAuditoria {
  entidad: EntidadAuditoria | '';
  accion: AccionAuditoria | '';
  entidadId: string;
  /** Días en formato AAAA-MM-DD (hora de Madrid), incluidos */
  desde: string;
  hasta: string;
  pagina: number;
}

export function buscarEnAuditoria(filtro: FiltroAuditoria, signal?: AbortSignal): Promise<PaginaAuditoria> {
  return api<PaginaAuditoria>('/api/auditoria', {
    parametros: {
      entidad: filtro.entidad,
      accion: filtro.accion,
      entidad_id: filtro.entidadId,
      desde: filtro.desde,
      hasta: filtro.hasta,
      pagina: filtro.pagina,
      tamano: TAMANO_PAGINA,
    },
    signal,
  });
}
