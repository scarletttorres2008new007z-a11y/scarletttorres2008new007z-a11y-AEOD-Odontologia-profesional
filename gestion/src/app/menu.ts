import { PERMISOS, type CodigoPermiso } from '../features/auth/permisos';
import type { NombreIcono } from '../shared/ui/Icono';

export interface OpcionMenu {
  ruta: string;
  texto: string;
  descripcion: string;
  icono: NombreIcono;
  /**
   * Sin este permiso la opción no aparece. Es solo para no ofrecer lo que no se puede usar:
   * la API rechaza igualmente (403) cualquier operación sin permiso.
   */
  permiso?: CodigoPermiso;
}

export interface SeccionMenu {
  titulo: string;
  opciones: OpcionMenu[];
}

export const INICIO: OpcionMenu = {
  ruta: '/',
  texto: 'Inicio',
  descripcion: 'Resumen y accesos directos.',
  icono: 'inicio',
};

// Cada fase añade aquí sus opciones (expedientes, facturación…).
export const SECCIONES: SeccionMenu[] = [
  {
    titulo: 'Clínica',
    opciones: [
      {
        ruta: '/agenda',
        texto: 'Agenda',
        descripcion: 'Ver las citas del día o de la semana y darlas, confirmarlas, moverlas o cancelarlas.',
        icono: 'agenda',
        permiso: PERMISOS.CITAS_VER,
      },
      {
        ruta: '/pacientes',
        texto: 'Pacientes',
        descripcion: 'Dar de alta pacientes, buscarlos y consultar o corregir sus datos de contacto.',
        icono: 'pacientes',
        permiso: PERMISOS.PACIENTES_VER,
      },
    ],
  },
  {
    titulo: 'Configuración',
    opciones: [
      {
        ruta: '/odontologos',
        texto: 'Odontólogos',
        descripcion:
          'Dar de alta odontólogos, cambiar lo que la web cuenta de ellos y vincular cada uno a su usuario.',
        icono: 'odontologos',
        permiso: PERMISOS.ODONTOLOGOS_GESTIONAR,
      },
      {
        ruta: '/tratamientos',
        texto: 'Tratamientos',
        descripcion: 'Cuánto dura cada tratamiento en la agenda, su precio orientativo y quién lo hace.',
        icono: 'tratamientos',
        permiso: PERMISOS.TRATAMIENTOS_GESTIONAR,
      },
      {
        ruta: '/horarios',
        texto: 'Horarios',
        descripcion: 'Cuándo abre la clínica y qué días y horas trabaja cada odontólogo.',
        icono: 'horarios',
        permiso: PERMISOS.HORARIOS_GESTIONAR,
      },
      {
        ruta: '/bloqueos',
        texto: 'Bloqueos',
        descripcion: 'Festivos, vacaciones, reuniones o capacitaciones: el tiempo en que no se dan citas.',
        icono: 'bloqueos',
        permiso: PERMISOS.BLOQUEOS_GESTIONAR,
      },
    ],
  },
  {
    titulo: 'Administración',
    opciones: [
      {
        ruta: '/usuarios',
        texto: 'Usuarios',
        descripcion: 'Dar acceso al personal, activar o desactivar usuarios y asignarles roles.',
        icono: 'usuarios',
        permiso: PERMISOS.USUARIOS_VER,
      },
      {
        ruta: '/roles',
        texto: 'Roles y permisos',
        descripcion: 'Decidir qué puede hacer cada rol dentro del software.',
        icono: 'roles',
        permiso: PERMISOS.ROLES_VER,
      },
      {
        ruta: '/auditoria',
        texto: 'Auditoría',
        descripcion: 'Consultar quién hizo cada operación importante y cuándo.',
        icono: 'auditoria',
        permiso: PERMISOS.AUDITORIA_VER,
      },
    ],
  },
];

/** Las secciones con solo las opciones que la persona puede usar (las secciones vacías no aparecen). */
export function seccionesVisibles(tienePermiso: (permiso: CodigoPermiso) => boolean): SeccionMenu[] {
  return SECCIONES.map((seccion) => ({
    ...seccion,
    opciones: seccion.opciones.filter((opcion) => !opcion.permiso || tienePermiso(opcion.permiso)),
  })).filter((seccion) => seccion.opciones.length > 0);
}
