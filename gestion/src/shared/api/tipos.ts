// Nombres cortos para los tipos que genera `npm run api:tipos` (esquema.ts) a partir de la API.
// Si cambia la API, se vuelve a generar esquema.ts y TypeScript avisa de lo que haya que adaptar.
import type { components } from './esquema';

type Esquemas = components['schemas'];

export type ErrorRespuesta = Esquemas['ErrorResponse'];
export type Sesion = Esquemas['SesionResponse'];
export type UsuarioActual = Esquemas['UsuarioActualResponse'];
export type Usuario = Esquemas['UsuarioResponse'];
export type UsuarioCreacion = Esquemas['UsuarioCreacionRequest'];
export type UsuarioEdicion = Esquemas['UsuarioEdicionRequest'];
export type PaginaUsuarios = Esquemas['PaginaResponseUsuarioResponse'];
export type Rol = Esquemas['RolResponse'];
export type RolResumen = Esquemas['RolResumen'];
export type Permiso = Esquemas['PermisoResponse'];
export type RegistroAuditoria = Esquemas['AuditoriaResponse'];
export type PaginaAuditoria = Esquemas['PaginaResponseAuditoriaResponse'];
export type AccionAuditoria = RegistroAuditoria['accion'];
export type EntidadAuditoria = RegistroAuditoria['entidad'];
export type OrigenAuditoria = RegistroAuditoria['origen'];
export type Paciente = Esquemas['PacienteResponse'];
export type DatosPaciente = Esquemas['PacienteRequest'];
export type PaginaPacientes = Esquemas['PaginaResponsePacienteResponse'];
export type TipoDocumento = NonNullable<Paciente['tipo_documento']>;
export type Sexo = NonNullable<Paciente['sexo']>;
