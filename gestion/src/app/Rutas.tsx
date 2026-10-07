import { Route, Routes } from 'react-router';
import { PaginaAgenda } from '../features/agenda/PaginaAgenda';
import { PaginaAuditoria } from '../features/auditoria/PaginaAuditoria';
import { useAuth } from '../features/auth/contexto';
import { PaginaEntrar } from '../features/auth/PaginaEntrar';
import { PERMISOS } from '../features/auth/permisos';
import { PaginaBloqueos } from '../features/configuracion/PaginaBloqueos';
import { PaginaHorarios } from '../features/configuracion/PaginaHorarios';
import { PaginaOdontologos } from '../features/configuracion/PaginaOdontologos';
import { PaginaTratamientos } from '../features/configuracion/PaginaTratamientos';
import { PaginaMiCuenta } from '../features/cuenta/PaginaMiCuenta';
import { PaginaInicio } from '../features/inicio/PaginaInicio';
import { PaginaPaciente } from '../features/pacientes/PaginaPaciente';
import { PaginaPacientes } from '../features/pacientes/PaginaPacientes';
import { PaginaRoles } from '../features/roles/PaginaRoles';
import { PaginaUsuarios } from '../features/usuarios/PaginaUsuarios';
import { Estructura } from './Estructura';
import { ConPermiso, ConSesion, SoloSinSesion } from './guardas';
import { PaginaNoEncontrada } from './PaginasDeAviso';
import { PantallaCargando, PantallaSinConexion } from './Pantallas';

export function Rutas() {
  const { estado, reintentar } = useAuth();

  if (estado === 'comprobando') return <PantallaCargando />;
  if (estado === 'sin-conexion') return <PantallaSinConexion alReintentar={reintentar} />;

  return (
    <Routes>
      <Route
        path="/entrar"
        element={
          <SoloSinSesion>
            <PaginaEntrar />
          </SoloSinSesion>
        }
      />
      <Route
        element={
          <ConSesion>
            <Estructura />
          </ConSesion>
        }
      >
        <Route index element={<PaginaInicio />} />
        <Route
          path="agenda"
          element={
            <ConPermiso permiso={PERMISOS.CITAS_VER}>
              <PaginaAgenda />
            </ConPermiso>
          }
        />
        <Route
          path="pacientes"
          element={
            <ConPermiso permiso={PERMISOS.PACIENTES_VER}>
              <PaginaPacientes />
            </ConPermiso>
          }
        />
        <Route
          path="pacientes/:id"
          element={
            <ConPermiso permiso={PERMISOS.PACIENTES_VER}>
              <PaginaPaciente />
            </ConPermiso>
          }
        />
        <Route
          path="odontologos"
          element={
            <ConPermiso permiso={PERMISOS.ODONTOLOGOS_GESTIONAR}>
              <PaginaOdontologos />
            </ConPermiso>
          }
        />
        <Route
          path="tratamientos"
          element={
            <ConPermiso permiso={PERMISOS.TRATAMIENTOS_GESTIONAR}>
              <PaginaTratamientos />
            </ConPermiso>
          }
        />
        <Route
          path="horarios"
          element={
            <ConPermiso permiso={PERMISOS.HORARIOS_GESTIONAR}>
              <PaginaHorarios />
            </ConPermiso>
          }
        />
        <Route
          path="bloqueos"
          element={
            <ConPermiso permiso={PERMISOS.BLOQUEOS_GESTIONAR}>
              <PaginaBloqueos />
            </ConPermiso>
          }
        />
        <Route
          path="usuarios"
          element={
            <ConPermiso permiso={PERMISOS.USUARIOS_VER}>
              <PaginaUsuarios />
            </ConPermiso>
          }
        />
        <Route
          path="roles"
          element={
            <ConPermiso permiso={PERMISOS.ROLES_VER}>
              <PaginaRoles />
            </ConPermiso>
          }
        />
        <Route
          path="auditoria"
          element={
            <ConPermiso permiso={PERMISOS.AUDITORIA_VER}>
              <PaginaAuditoria />
            </ConPermiso>
          }
        />
        <Route path="mi-cuenta" element={<PaginaMiCuenta />} />
        <Route path="*" element={<PaginaNoEncontrada />} />
      </Route>
    </Routes>
  );
}
