import type {
  AccionAuditoria,
  EntidadAuditoria,
  OrigenAuditoria,
  RegistroAuditoria,
} from '../../shared/api/tipos';
import { formatearFecha, formatearFechaHora, formatearTelefono } from '../../shared/formato';

// Textos en castellano para los códigos que guarda la auditoría.

export const ACCIONES: Record<AccionAuditoria, string> = {
  INICIAR_SESION: 'Inicio de sesión',
  INICIO_SESION_FALLIDO: 'Intento de entrada fallido',
  BLOQUEAR_POR_INTENTOS: 'Bloqueo por intentos fallidos',
  CERRAR_SESION: 'Cierre de sesión',
  CAMBIAR_PASSWORD: 'Cambio de su contraseña',
  RESTABLECER_PASSWORD: 'Contraseña restablecida',
  CREAR: 'Alta',
  EDITAR: 'Edición de datos',
  ACTIVAR: 'Activación o desbloqueo',
  DESACTIVAR: 'Desactivación',
  CAMBIAR_ROLES: 'Cambio de roles',
  CAMBIAR_PERMISOS: 'Cambio de permisos',
  RESERVAR: 'Cita reservada o dada',
  CAMBIAR_ESTADO: 'Cambio de estado de cita',
  VINCULAR_PACIENTE: 'Cita vinculada a un paciente',
  CANCELAR: 'Cancelación de cita',
  REPROGRAMAR: 'Cita movida de día u hora',
};

/** Para el filtro de acciones, agrupadas. */
export const GRUPOS_DE_ACCIONES: { titulo: string; acciones: AccionAuditoria[] }[] = [
  {
    titulo: 'Sesiones',
    acciones: [
      'INICIAR_SESION',
      'INICIO_SESION_FALLIDO',
      'BLOQUEAR_POR_INTENTOS',
      'CERRAR_SESION',
      'CAMBIAR_PASSWORD',
    ],
  },
  { titulo: 'Altas y cambios', acciones: ['CREAR', 'EDITAR', 'ACTIVAR', 'DESACTIVAR'] },
  { titulo: 'Usuarios y roles', acciones: ['CAMBIAR_ROLES', 'RESTABLECER_PASSWORD', 'CAMBIAR_PERMISOS'] },
  {
    titulo: 'Citas',
    acciones: ['RESERVAR', 'CAMBIAR_ESTADO', 'VINCULAR_PACIENTE', 'CANCELAR', 'REPROGRAMAR'],
  },
];

export const ENTIDADES: Record<EntidadAuditoria, string> = {
  USUARIO: 'Usuario',
  ROL: 'Rol',
  CITA: 'Cita',
  PACIENTE: 'Paciente',
  ODONTOLOGO: 'Odontólogo',
  TRATAMIENTO: 'Tratamiento',
  HORARIO_CLINICA: 'Horario de la clínica',
  HORARIO_ODONTOLOGO: 'Turnos del odontólogo',
  BLOQUEO: 'Bloqueo',
};

export const ENTIDADES_EN_PLURAL: Record<EntidadAuditoria, string> = {
  USUARIO: 'Usuarios',
  ROL: 'Roles',
  CITA: 'Citas',
  PACIENTE: 'Pacientes',
  ODONTOLOGO: 'Odontólogos',
  TRATAMIENTO: 'Tratamientos',
  HORARIO_CLINICA: 'Horario de la clínica',
  HORARIO_ODONTOLOGO: 'Turnos de los odontólogos',
  BLOQUEO: 'Bloqueos',
};

export const ORIGENES: Record<OrigenAuditoria, string> = {
  LANDING: 'Web',
  SOFTWARE: 'Software',
  APP: 'App de pacientes',
  SISTEMA: 'Sistema',
};

const CAMPOS: Record<string, string> = {
  nombre: 'Nombre',
  username: 'Usuario',
  email: 'Correo',
  activo: 'Activo',
  bloqueado: 'Bloqueado',
  bloqueado_hasta: 'Bloqueado hasta',
  roles: 'Roles',
  permisos: 'Permisos',
  motivo: 'Motivo',
  estado: 'Estado',
  fecha: 'Fecha',
  hora_inicio: 'Hora de inicio',
  hora_fin: 'Hora de fin',
  tratamiento: 'Tratamiento',
  odontologo: 'Odontólogo',
  paciente: 'Paciente (código)',
  notas_internas: 'Notas internas',
  tratamiento_id: 'Tratamiento (n.º)',
  odontologo_id: 'Odontólogo (n.º)',
  cita_id: 'Cita nueva (n.º)',
  codigo: 'Código',
  nombres: 'Nombre',
  apellidos: 'Apellidos',
  tipo_documento: 'Tipo de documento',
  numero_documento: 'N.º de documento',
  fecha_nacimiento: 'Fecha de nacimiento',
  sexo: 'Sexo',
  telefono: 'Teléfono',
  direccion: 'Dirección',
  contacto_emergencia_nombre: 'Contacto de emergencia',
  contacto_emergencia_telefono: 'Teléfono de emergencia',
  observaciones: 'Observaciones',
  // Configuración de la agenda
  especialidad: 'Especialidad',
  descripcion: 'Presentación',
  usuario: 'Usuario del software',
  descripcion_corta: 'Descripción corta',
  precio_desde: 'Precio desde (€)',
  duracion_aproximada: 'Duración orientativa',
  duracion_minutos: 'Duración en la agenda (min)',
  odontologos: 'Quién lo hace',
  tipo: 'Tipo',
  fecha_inicio: 'Primer día',
  fecha_fin: 'Último día',
  dia_semana: 'Cada semana, el',
  horas: 'Horas',
  lunes: 'Lunes',
  martes: 'Martes',
  miércoles: 'Miércoles',
  jueves: 'Jueves',
  viernes: 'Viernes',
  sábado: 'Sábado',
  domingo: 'Domingo',
};

const VALORES: Record<string, string> = {
  USUARIO_DESCONOCIDO: 'Usuario o correo que no existe',
  PASSWORD_INCORRECTA: 'Contraseña incorrecta',
  ADMINISTRADOR: 'Administrador',
  RECEPCION: 'Recepción',
  ODONTOLOGO: 'Odontólogo',
  COORDINADOR: 'Coordinador',
  PENDIENTE: 'Por confirmar',
  CONFIRMADA: 'Confirmada',
  EN_ATENCION: 'En consulta',
  CANCELADA: 'Cancelada',
  REPROGRAMADA: 'Reprogramada',
  COMPLETADA: 'Completada',
  NO_ASISTIO: 'No asistió',
  PASAPORTE: 'Pasaporte',
  OTRO: 'Otro',
  MUJER: 'Mujer',
  HOMBRE: 'Hombre',
  ALMUERZO: 'Almuerzo',
  REUNION: 'Reunión',
  CAPACITACION: 'Capacitación',
  MANTENIMIENTO: 'Mantenimiento',
  VACACIONES: 'Vacaciones',
  FERIADO: 'Festivo',
  MANUAL: 'No disponible',
};

const CLAVES_CON_CODIGO = new Set(['motivo', 'roles', 'estado', 'tipo_documento', 'sexo', 'tipo']);

export function nombreDelCampo(clave: string): string {
  return CAMPOS[clave] ?? clave;
}

/** Un valor guardado en la auditoría, listo para leer. */
export function textoDelValor(clave: string, valor: unknown): string {
  if (valor === null || valor === undefined) return '—';
  if (typeof valor === 'boolean') return valor ? 'Sí' : 'No';
  if (Array.isArray(valor)) {
    return valor.length === 0 ? 'Ninguno' : valor.map((item) => textoDelValor(clave, item)).join(', ');
  }
  if (typeof valor === 'string') {
    if (['fecha', 'fecha_nacimiento', 'fecha_inicio', 'fecha_fin'].includes(clave))
      return formatearFecha(valor);
    if (clave === 'bloqueado_hasta') return formatearFechaHora(valor);
    if (clave === 'telefono' || clave === 'contacto_emergencia_telefono') return formatearTelefono(valor);
    // Solo se traducen los datos que guardan un código; un nombre o una observación se muestran tal cual
    return CLAVES_CON_CODIGO.has(clave) ? (VALORES[valor] ?? valor) : valor;
  }
  if (typeof valor === 'number') return String(valor);
  return JSON.stringify(valor);
}

/** Quién lo hizo: la persona con sesión o, si no había, de dónde llegó. */
export function autor(registro: RegistroAuditoria): { nombre: string; detalle?: string } {
  if (registro.usuario) return { nombre: registro.usuario.nombre, detalle: registro.usuario.username };
  switch (registro.origen) {
    case 'LANDING':
      return { nombre: 'Visitante de la web' };
    case 'APP':
      return { nombre: 'App de pacientes' };
    case 'SISTEMA':
      return { nombre: 'Sistema' };
    default:
      return { nombre: 'Sin identificar' };
  }
}

/** La acción, con el nombre que tiene para los pacientes (alta, baja y reactivación). */
export function textoDeAccion(registro: RegistroAuditoria): string {
  if (registro.entidad === 'PACIENTE') {
    if (registro.accion === 'ACTIVAR') return 'Reactivación';
    if (registro.accion === 'DESACTIVAR') return 'Baja';
  }
  return ACCIONES[registro.accion];
}

export function afectado(registro: RegistroAuditoria): string {
  const entidad = ENTIDADES[registro.entidad];
  return registro.entidad_id ? `${entidad} n.º ${registro.entidad_id}` : entidad;
}
