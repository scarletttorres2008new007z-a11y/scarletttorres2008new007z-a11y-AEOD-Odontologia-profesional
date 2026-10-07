-- V4 · Fase 3: agenda y citas en el software de gestión.
-- La tabla citas sigue siendo la única tabla de citas: la landing, el software y la futura app escriben en ella.
-- Las citas que ya existen no pierden nada: las columnas nuevas son opcionales.
-- Compatible con MySQL 8.4 (producción) y con la MariaDB de XAMPP (10.4 o superior).

-- ─────────────── Citas ───────────────

ALTER TABLE citas
    ADD COLUMN paciente_id BIGINT NULL
        COMMENT 'Ficha del paciente. Las citas de la web llegan sin ella hasta que recepción las vincula' AFTER odontologo_id,
    ADD COLUMN notas_internas VARCHAR(1000) NULL COMMENT 'Solo para el personal; el paciente no las ve' AFTER mensaje,
    ADD COLUMN creada_por_usuario_id BIGINT NULL COMMENT 'Quién la dio desde el software; vacío = la web' AFTER origen,
    MODIFY email VARCHAR(150) NULL COMMENT 'Obligatorio en la web; en el software, el del paciente si lo tiene',
    ADD CONSTRAINT fk_citas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    ADD CONSTRAINT fk_citas_creada_por FOREIGN KEY (creada_por_usuario_id) REFERENCES usuarios (id);

-- La agenda y las listas buscan por día y hora
CREATE INDEX ix_citas_fecha ON citas (fecha, hora_inicio);

-- Nuevo estado EN_ATENCION y orígenes con los nombres del plan: CLINICA pasa a SOFTWARE y APP_PACIENTE a APP
ALTER TABLE citas DROP CONSTRAINT ck_citas_estado;
ALTER TABLE citas DROP CONSTRAINT ck_citas_origen;
ALTER TABLE citas DROP CONSTRAINT ck_citas_cancelada_por;

UPDATE citas SET origen = 'SOFTWARE' WHERE origen = 'CLINICA';
UPDATE citas SET origen = 'APP' WHERE origen = 'APP_PACIENTE';
UPDATE citas SET cancelada_por = 'SOFTWARE' WHERE cancelada_por = 'CLINICA';
UPDATE citas SET cancelada_por = 'APP' WHERE cancelada_por = 'APP_PACIENTE';

ALTER TABLE citas
    ADD CONSTRAINT ck_citas_estado CHECK (estado IN
        ('PENDIENTE', 'CONFIRMADA', 'EN_ATENCION', 'COMPLETADA', 'NO_ASISTIO', 'CANCELADA', 'REPROGRAMADA')),
    ADD CONSTRAINT ck_citas_origen CHECK (origen IN ('LANDING', 'SOFTWARE', 'APP')),
    ADD CONSTRAINT ck_citas_cancelada_por CHECK (cancelada_por IS NULL OR cancelada_por IN ('LANDING', 'SOFTWARE', 'APP'));

ALTER TABLE lista_espera DROP CONSTRAINT ck_lista_espera_origen;
UPDATE lista_espera SET origen = 'SOFTWARE' WHERE origen = 'CLINICA';
UPDATE lista_espera SET origen = 'APP' WHERE origen = 'APP_PACIENTE';
ALTER TABLE lista_espera ADD CONSTRAINT ck_lista_espera_origen CHECK (origen IN ('LANDING', 'SOFTWARE', 'APP'));

-- ─────────────── Odontólogos ───────────────

-- Un odontólogo puede tener su usuario del software: así ve su propia agenda
ALTER TABLE odontologos
    ADD COLUMN usuario_id BIGINT NULL COMMENT 'Su usuario del software (vacío = sin acceso a su agenda)',
    ADD CONSTRAINT uk_odontologo_usuario UNIQUE (usuario_id),
    ADD CONSTRAINT fk_odontologos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

-- ─────────────── Permisos ───────────────

INSERT INTO permisos (codigo, modulo, descripcion) VALUES
    ('citas.ver', 'Agenda y citas',
     'Ver la agenda y las citas. Sin «Ver todas las agendas», solo las del odontólogo vinculado a su usuario'),
    ('citas.ver_todas', 'Agenda y citas', 'Ver la agenda y las citas de todos los odontólogos'),
    ('citas.crear', 'Agenda y citas', 'Dar citas a pacientes'),
    ('citas.cambiar_estado', 'Agenda y citas',
     'Confirmar citas y marcar si el paciente está en consulta, ha terminado o no ha venido'),
    ('citas.cancelar', 'Agenda y citas', 'Cancelar citas'),
    ('citas.reprogramar', 'Agenda y citas', 'Cambiar una cita de día, hora u odontólogo'),
    ('citas.editar', 'Agenda y citas', 'Vincular una cita a la ficha de un paciente y escribir sus notas internas');

-- El administrador tiene todos los permisos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR' AND p.codigo LIKE 'citas.%';

-- Reparto inicial del plan; se cambia desde «Roles y permisos» sin tocar código.
-- Recepción y coordinación llevan la agenda de todos; el odontólogo ve la suya.
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo IN ('RECEPCION', 'COORDINADOR') AND p.codigo LIKE 'citas.%';

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'ODONTOLOGO' AND p.codigo = 'citas.ver';
