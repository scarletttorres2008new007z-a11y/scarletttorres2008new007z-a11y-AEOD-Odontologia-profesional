/**
 * Permisos que existen en la API (tabla permisos). El software los usa solo para decidir qué mostrar:
 * quien protege de verdad cada operación es el backend, que responde 403 si falta el permiso.
 */
export const PERMISOS = {
  USUARIOS_VER: 'usuarios.ver',
  USUARIOS_CREAR: 'usuarios.crear',
  USUARIOS_EDITAR: 'usuarios.editar',
  USUARIOS_ASIGNAR_ROLES: 'usuarios.asignar_roles',
  ROLES_VER: 'roles.ver',
  ROLES_EDITAR: 'roles.editar',
  AUDITORIA_VER: 'auditoria.ver',
  PACIENTES_VER: 'pacientes.ver',
  PACIENTES_CREAR: 'pacientes.crear',
  PACIENTES_EDITAR: 'pacientes.editar',
} as const;

export type CodigoPermiso = (typeof PERMISOS)[keyof typeof PERMISOS];
