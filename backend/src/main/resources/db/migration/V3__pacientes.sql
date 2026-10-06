-- V3 · Fase 2: pacientes, solo con sus datos administrativos.
-- La información clínica NO va en esta tabla: irá en el expediente (Fase 4), que colgará del paciente.
-- Compatible con MySQL 8.4 (producción) y con la MariaDB de XAMPP (10.4 o superior).

CREATE TABLE pacientes (
    id                           BIGINT        NOT NULL AUTO_INCREMENT,
    codigo                       VARCHAR(10)   NOT NULL COMMENT 'Código del paciente para la clínica (ej.: K7M3QX). Aleatorio: no se puede adivinar',
    nombres                      VARCHAR(100)  NOT NULL,
    apellidos                    VARCHAR(100)  NOT NULL,
    tipo_documento               VARCHAR(20)   COMMENT 'DNI, NIE, PASAPORTE u OTRO',
    numero_documento             VARCHAR(20)   COMMENT 'En mayúsculas, sin espacios ni guiones',
    fecha_nacimiento             DATE,
    sexo                         VARCHAR(10)   COMMENT 'MUJER, HOMBRE u OTRO; vacío si no se indica',
    telefono                     VARCHAR(20)   NOT NULL COMMENT 'Solo dígitos, con + delante si lleva prefijo internacional',
    email                        VARCHAR(150)  COMMENT 'En minúsculas',
    direccion                    VARCHAR(255),
    contacto_emergencia_nombre   VARCHAR(100),
    contacto_emergencia_telefono VARCHAR(20),
    observaciones                VARCHAR(1000) COMMENT 'Solo datos administrativos; nunca información clínica',
    activo                       BIT(1)        NOT NULL COMMENT 'Un paciente dado de baja no se borra nunca',
    creado_en                    DATETIME(6)   NOT NULL,
    actualizado_en               DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_paciente_codigo UNIQUE (codigo),
    CONSTRAINT uk_paciente_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_pacientes_tipo_documento CHECK (tipo_documento IN ('DNI', 'NIE', 'PASAPORTE', 'OTRO')),
    CONSTRAINT ck_pacientes_documento_completo CHECK ((tipo_documento IS NULL) = (numero_documento IS NULL)),
    CONSTRAINT ck_pacientes_sexo CHECK (sexo IN ('MUJER', 'HOMBRE', 'OTRO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Pacientes de la clínica: datos personales y de contacto. Lo clínico va en el expediente';

-- El listado se ordena por apellidos y nombre
CREATE INDEX ix_pacientes_nombre ON pacientes (apellidos, nombres);

-- ─────────────── Permisos ───────────────

INSERT INTO permisos (codigo, modulo, descripcion) VALUES
    ('pacientes.ver', 'Pacientes', 'Buscar pacientes y ver sus datos personales y de contacto'),
    ('pacientes.crear', 'Pacientes', 'Dar de alta pacientes'),
    ('pacientes.editar', 'Pacientes', 'Editar los datos de un paciente, darlo de baja o reactivarlo');

-- El administrador tiene todos los permisos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR' AND p.codigo LIKE 'pacientes.%';

-- Reparto inicial del plan; se cambia desde «Roles y permisos» sin tocar código.
-- Recepción y coordinación dan de alta y editan pacientes; el odontólogo los consulta.
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo IN ('RECEPCION', 'COORDINADOR') AND p.codigo LIKE 'pacientes.%';

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'ODONTOLOGO' AND p.codigo = 'pacientes.ver';
