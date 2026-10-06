-- V2 · Fase 1: usuarios, roles, permisos, sesiones y auditoría del software de gestión.
-- Autorización: usuario → roles → permisos. El backend comprueba un permiso concreto en cada endpoint privado.
-- Compatible con MySQL 8.4 (producción) y con la MariaDB de XAMPP (10.4 o superior).

-- ─────────────── Usuarios, roles y permisos ───────────────

CREATE TABLE usuarios (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    username          VARCHAR(50)   NOT NULL COMMENT 'Nombre de usuario para entrar, en minúsculas',
    email             VARCHAR(150)  NOT NULL COMMENT 'También sirve para entrar, en minúsculas',
    password_hash     VARCHAR(100)  NOT NULL COMMENT 'Contraseña cifrada con BCrypt; nunca se guarda en claro',
    nombre            VARCHAR(100)  NOT NULL,
    activo            BIT(1)        NOT NULL COMMENT 'Un usuario desactivado no puede entrar; no se borra nunca',
    intentos_fallidos INT           NOT NULL COMMENT 'Contraseñas incorrectas seguidas',
    bloqueado_hasta   DATETIME(6)   COMMENT 'Bloqueo temporal tras demasiados intentos fallidos',
    ultimo_acceso     DATETIME(6),
    creado_en         DATETIME(6)   NOT NULL,
    actualizado_en    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuarios_intentos CHECK (intentos_fallidos >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Personal de la clínica con acceso al software de gestión';

CREATE TABLE roles (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    codigo      VARCHAR(30)   NOT NULL,
    nombre      VARCHAR(60)   NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_rol_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Roles del personal. Se pueden añadir más sin cambiar el código';

CREATE TABLE permisos (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    codigo      VARCHAR(60)   NOT NULL COMMENT 'modulo.accion, por ejemplo usuarios.crear',
    modulo      VARCHAR(60)   NOT NULL COMMENT 'Módulo del software al que pertenece, para agruparlos',
    descripcion VARCHAR(255)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permiso_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Acciones concretas que se pueden permitir a un rol. Cada fase añade las suyas con una migración';

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Roles de cada usuario';

CREATE TABLE rol_permisos (
    rol_id     BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permisos_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_rol_permisos_permiso FOREIGN KEY (permiso_id) REFERENCES permisos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Permisos de cada rol';

-- ─────────────── Sesiones ───────────────

-- Una fila por cada inicio de sesión. El navegador guarda el token de refresco en una cookie HttpOnly;
-- aquí solo queda su huella SHA-256, así que una copia de la base no permite entrar.
CREATE TABLE sesiones (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id         BIGINT        NOT NULL,
    refresh_token_hash CHAR(64)      NOT NULL COMMENT 'SHA-256 del token de refresco',
    creada_en          DATETIME(6)   NOT NULL,
    expira_en          DATETIME(6)   NOT NULL,
    revocada_en        DATETIME(6)   COMMENT 'Cierre de sesión, cambio de contraseña o usuario desactivado',
    ip                 VARCHAR(45),
    user_agent         VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_sesion_refresh_token UNIQUE (refresh_token_hash),
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Sesiones del software de gestión; se pueden revocar en cualquier momento';

-- ─────────────── Auditoría ───────────────

CREATE TABLE auditoria (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id     BIGINT        COMMENT 'Quién lo hizo; vacío si fue un paciente desde la web o el propio sistema',
    origen         VARCHAR(20)   NOT NULL COMMENT 'Desde dónde: LANDING, SOFTWARE, APP o SISTEMA',
    accion         VARCHAR(40)   NOT NULL,
    entidad        VARCHAR(40)   NOT NULL COMMENT 'Sobre qué: USUARIO, ROL, CITA…',
    entidad_id     VARCHAR(64)   COMMENT 'Identificador del registro afectado',
    valor_anterior TEXT          COMMENT 'Datos antes del cambio (JSON)',
    valor_nuevo    TEXT          COMMENT 'Datos después del cambio (JSON)',
    ip             VARCHAR(45),
    creado_en      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_auditoria_origen CHECK (origen IN ('LANDING', 'SOFTWARE', 'APP', 'SISTEMA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro de operaciones importantes. Solo se añaden filas; nunca se cambian ni se borran';

CREATE INDEX ix_auditoria_entidad ON auditoria (entidad, entidad_id);
CREATE INDEX ix_auditoria_creado_en ON auditoria (creado_en);

-- ─────────────── Datos de partida ───────────────

INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Administrador', 'Acceso completo: usuarios, roles, permisos y auditoría. Sus permisos no se pueden quitar.'),
    ('RECEPCION', 'Recepción', 'Atención a pacientes y agenda.'),
    ('ODONTOLOGO', 'Odontólogo', 'Su agenda y la atención de sus pacientes.'),
    ('COORDINADOR', 'Coordinador', 'Organización de la agenda, el equipo y los tratamientos.');

INSERT INTO permisos (codigo, modulo, descripcion) VALUES
    ('usuarios.ver', 'Usuarios', 'Ver los usuarios y sus datos'),
    ('usuarios.crear', 'Usuarios', 'Crear usuarios'),
    ('usuarios.editar', 'Usuarios', 'Editar usuarios, activarlos o desactivarlos y restablecer su contraseña'),
    ('usuarios.asignar_roles', 'Usuarios', 'Cambiar los roles de un usuario'),
    ('roles.ver', 'Roles y permisos', 'Ver los roles y sus permisos'),
    ('roles.editar', 'Roles y permisos', 'Cambiar los permisos de un rol'),
    ('auditoria.ver', 'Auditoría', 'Consultar el registro de auditoría');

-- El administrador tiene todos los permisos. Las migraciones de cada fase le dan también los permisos nuevos.
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p WHERE r.codigo = 'ADMINISTRADOR';
