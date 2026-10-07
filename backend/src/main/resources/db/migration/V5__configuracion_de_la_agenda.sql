-- V5 · Fase 3, parte 2: configurar la agenda desde el software de gestión.
-- Las tablas de odontólogos, tratamientos, horarios y bloqueos ya existen (V1): aquí solo se añaden el tipo de bloqueo
-- de capacitación y los permisos para cambiarlos. Los datos que ya hay no cambian.
-- Compatible con MySQL 8.4 (producción) y con la MariaDB de XAMPP (10.4 o superior).

-- ─────────────── Bloqueos: capacitaciones ───────────────

ALTER TABLE bloqueos DROP CONSTRAINT ck_bloqueos_tipo;
ALTER TABLE bloqueos ADD CONSTRAINT ck_bloqueos_tipo CHECK (tipo IN
    ('ALMUERZO', 'REUNION', 'CAPACITACION', 'MANTENIMIENTO', 'VACACIONES', 'FERIADO', 'MANUAL'));

-- ─────────────── Permisos ───────────────

INSERT INTO permisos (codigo, modulo, descripcion) VALUES
    ('odontologos.gestionar', 'Configuración de la agenda',
     'Dar de alta odontólogos, cambiar sus datos, activarlos o desactivarlos y vincularlos a su usuario del software'),
    ('tratamientos.gestionar', 'Configuración de la agenda',
     'Dar de alta tratamientos y cambiar su duración, su precio, quién los hace y si se ofrecen'),
    ('horarios.gestionar', 'Configuración de la agenda',
     'Cambiar el horario de la clínica y los turnos de cada odontólogo'),
    ('bloqueos.gestionar', 'Configuración de la agenda',
     'Bloquear tiempo en la agenda: festivos, vacaciones, reuniones, capacitaciones, mantenimiento…');

-- El administrador tiene todos los permisos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR'
  AND p.codigo IN ('odontologos.gestionar', 'tratamientos.gestionar', 'horarios.gestionar', 'bloqueos.gestionar');

-- Reparto inicial; se cambia desde «Roles y permisos» sin tocar código.
-- Coordinación organiza la agenda; recepción puede bloquear tiempo (por ejemplo, si un odontólogo no puede venir).
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'COORDINADOR'
  AND p.codigo IN ('odontologos.gestionar', 'tratamientos.gestionar', 'horarios.gestionar', 'bloqueos.gestionar');

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'RECEPCION' AND p.codigo = 'bloqueos.gestionar';
