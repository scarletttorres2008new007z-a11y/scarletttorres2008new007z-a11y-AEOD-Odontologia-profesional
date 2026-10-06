-- V1 · Esquema inicial de la base central.
-- Son las tablas que ya usaba la landing (tratamientos, equipo, agenda, citas y contacto),
-- ahora escritas a mano en lugar de generadas por Hibernate.
-- Compatible con MySQL 8.4 (producción) y con la MariaDB de XAMPP (10.4 o superior).
-- Una migración aplicada no se modifica nunca: cualquier cambio va en un archivo V2, V3…

-- Textos en utf8mb4 (tildes, ñ y cualquier carácter) también para las tablas futuras.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ─────────────── Catálogo ───────────────

CREATE TABLE tratamientos (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(120)  NOT NULL,
    descripcion         VARCHAR(2000),
    descripcion_corta   VARCHAR(255),
    precio_desde        DECIMAL(10,2) COMMENT 'Precio orientativo "desde", en euros',
    duracion_aproximada VARCHAR(80)   COMMENT 'Texto para mostrar, por ejemplo "30-45 min"',
    duracion_minutos    INT           COMMENT 'Minutos que ocupa la cita en la agenda',
    imagen              VARCHAR(500),
    activo              BIT(1)        NOT NULL,
    orden               INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_tratamientos_duracion CHECK (duracion_minutos IS NULL OR duracion_minutos > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tratamientos que ofrece la clínica';

CREATE TABLE odontologos (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(120)  NOT NULL,
    especialidad VARCHAR(150),
    descripcion  VARCHAR(1000),
    imagen       VARCHAR(500),
    activo       BIT(1)        NOT NULL,
    orden        INT           NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Equipo de odontólogos';

CREATE TABLE odontologo_tratamientos (
    odontologo_id  BIGINT NOT NULL,
    tratamiento_id BIGINT NOT NULL,
    PRIMARY KEY (odontologo_id, tratamiento_id),
    CONSTRAINT fk_odontologo_tratamientos_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologos (id),
    CONSTRAINT fk_odontologo_tratamientos_tratamiento FOREIGN KEY (tratamiento_id) REFERENCES tratamientos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Qué odontólogo hace cada tratamiento. Un tratamiento sin filas lo hace cualquiera';

-- ─────────────── Horarios y bloqueos ───────────────

CREATE TABLE horarios_clinica (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    dia_semana    INT    NOT NULL COMMENT '1 = lunes … 7 = domingo',
    hora_apertura TIME   NOT NULL,
    hora_cierre   TIME   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_horario_clinica_dia UNIQUE (dia_semana),
    CONSTRAINT ck_horarios_clinica_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horarios_clinica_horas CHECK (hora_cierre > hora_apertura)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Horario de apertura por día. Un día sin fila está cerrado';

CREATE TABLE horarios_odontologo (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    odontologo_id BIGINT NOT NULL,
    dia_semana    INT    NOT NULL COMMENT '1 = lunes … 7 = domingo',
    hora_inicio   TIME   NOT NULL,
    hora_fin      TIME   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_horarios_odontologo_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologos (id),
    CONSTRAINT ck_horarios_odontologo_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horarios_odontologo_horas CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Turnos de cada odontólogo; puede haber varios el mismo día';

CREATE TABLE bloqueos (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    tipo          VARCHAR(20)  NOT NULL,
    motivo        VARCHAR(150),
    odontologo_id BIGINT       COMMENT 'Vacío = toda la clínica',
    fecha_inicio  DATE         COMMENT 'Vacías = siempre',
    fecha_fin     DATE,
    dia_semana    INT          COMMENT 'Vacío = todos los días; 1 = lunes … 7 = domingo',
    hora_inicio   TIME         COMMENT 'Vacías = el día entero',
    hora_fin      TIME,
    activo        BIT(1)       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bloqueos_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologos (id),
    CONSTRAINT ck_bloqueos_tipo CHECK (tipo IN ('ALMUERZO', 'REUNION', 'MANTENIMIENTO', 'VACACIONES', 'FERIADO', 'MANUAL')),
    CONSTRAINT ck_bloqueos_dia CHECK (dia_semana IS NULL OR dia_semana BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tiempo no disponible: almuerzo, reuniones, mantenimiento, vacaciones, feriados';

-- ─────────────── Citas ───────────────

CREATE TABLE citas (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    codigo             VARCHAR(36)   NOT NULL COMMENT 'Referencia pública; el id interno nunca sale de la API',
    tratamiento_id     BIGINT        NOT NULL,
    odontologo_id      BIGINT        NOT NULL,
    fecha              DATE          NOT NULL,
    hora_inicio        TIME          NOT NULL,
    hora_fin           TIME          NOT NULL,
    nombre             VARCHAR(100)  NOT NULL,
    telefono           VARCHAR(20)   NOT NULL,
    email              VARCHAR(150)  NOT NULL,
    mensaje            VARCHAR(1000),
    estado             VARCHAR(20)   NOT NULL,
    origen             VARCHAR(20)   NOT NULL COMMENT 'Desde dónde se creó: landing, software o app',
    cita_anterior_id   BIGINT        COMMENT 'Cita a la que sustituye (reprogramación)',
    creada_en          DATETIME(6)   NOT NULL,
    actualizada_en     DATETIME(6)   NOT NULL,
    cancelada_en       DATETIME(6),
    cancelada_por      VARCHAR(20),
    motivo_cancelacion VARCHAR(255),
    version            BIGINT        NOT NULL COMMENT 'Control de cambios simultáneos sobre la misma cita',
    PRIMARY KEY (id),
    CONSTRAINT uk_cita_codigo UNIQUE (codigo),
    CONSTRAINT fk_citas_tratamiento FOREIGN KEY (tratamiento_id) REFERENCES tratamientos (id),
    CONSTRAINT fk_citas_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologos (id),
    CONSTRAINT fk_citas_cita_anterior FOREIGN KEY (cita_anterior_id) REFERENCES citas (id),
    CONSTRAINT ck_citas_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_citas_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'REPROGRAMADA', 'COMPLETADA', 'NO_ASISTIO')),
    CONSTRAINT ck_citas_origen CHECK (origen IN ('LANDING', 'CLINICA', 'APP_PACIENTE')),
    CONSTRAINT ck_citas_cancelada_por CHECK (cancelada_por IS NULL OR cancelada_por IN ('LANDING', 'CLINICA', 'APP_PACIENTE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Única tabla de citas para la landing, el software y la futura app. Nunca se borran filas';

CREATE INDEX ix_cita_odontologo_fecha ON citas (odontologo_id, fecha);

-- Garantía contra la doble reserva: una fila por cada 15 minutos ocupados por una cita activa.
-- Si dos reservas que se solapan llegan a la vez, la clave única solo deja entrar a una.
CREATE TABLE agenda_ocupacion (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    odontologo_id BIGINT NOT NULL,
    fecha         DATE   NOT NULL,
    hora          TIME   NOT NULL,
    cita_id       BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_agenda_ocupacion UNIQUE (odontologo_id, fecha, hora),
    CONSTRAINT fk_agenda_ocupacion_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologos (id),
    CONSTRAINT fk_agenda_ocupacion_cita FOREIGN KEY (cita_id) REFERENCES citas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tramos de 15 minutos ocupados; impide la doble reserva';

CREATE TABLE lista_espera (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tratamiento_id BIGINT       NOT NULL,
    odontologo_id  BIGINT       COMMENT 'Vacío = cualquier odontólogo',
    fecha_desde    DATE         NOT NULL,
    fecha_hasta    DATE         NOT NULL,
    franja         VARCHAR(10)  COMMENT 'Vacío = cualquier hora',
    nombre         VARCHAR(100) NOT NULL,
    telefono       VARCHAR(20)  NOT NULL,
    email          VARCHAR(150) NOT NULL,
    estado         VARCHAR(20)  NOT NULL,
    origen         VARCHAR(20)  NOT NULL,
    creada_en      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lista_espera_tratamiento FOREIGN KEY (tratamiento_id) REFERENCES tratamientos (id),
    CONSTRAINT fk_lista_espera_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologos (id),
    CONSTRAINT ck_lista_espera_franja CHECK (franja IS NULL OR franja IN ('MANANA', 'TARDE')),
    CONSTRAINT ck_lista_espera_estado CHECK (estado IN ('ACTIVA', 'AVISADA', 'ATENDIDA', 'CANCELADA')),
    CONSTRAINT ck_lista_espera_origen CHECK (origen IN ('LANDING', 'CLINICA', 'APP_PACIENTE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Preparada para la lista de espera; todavía sin pantalla';

-- Bandeja de avisos ("outbox"): se escribe en la misma transacción que la cita. Hoy no se envía nada.
CREATE TABLE notificaciones (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tipo            VARCHAR(30)  NOT NULL,
    destinatario    VARCHAR(20)  NOT NULL,
    odontologo_id   BIGINT,
    cita_id         BIGINT,
    lista_espera_id BIGINT,
    detalle         VARCHAR(500),
    canal           VARCHAR(20)  COMMENT 'Vacío mientras está pendiente',
    estado          VARCHAR(20)  NOT NULL,
    creada_en       DATETIME(6)  NOT NULL,
    enviada_en      DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notificaciones_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologos (id),
    CONSTRAINT fk_notificaciones_cita FOREIGN KEY (cita_id) REFERENCES citas (id),
    CONSTRAINT fk_notificaciones_lista_espera FOREIGN KEY (lista_espera_id) REFERENCES lista_espera (id),
    CONSTRAINT ck_notificaciones_tipo CHECK (tipo IN ('NUEVA_CITA', 'CITA_CANCELADA', 'CITA_REPROGRAMADA', 'HUECO_LIBERADO')),
    CONSTRAINT ck_notificaciones_destinatario CHECK (destinatario IN ('ODONTOLOGO', 'PACIENTE', 'LISTA_ESPERA', 'CLINICA')),
    CONSTRAINT ck_notificaciones_estado CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'ERROR', 'DESCARTADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Avisos pendientes de enviar';

CREATE INDEX ix_notificacion_estado ON notificaciones (estado);

-- ─────────────── Contacto ───────────────

CREATE TABLE solicitudes_contacto (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100)  NOT NULL,
    telefono    VARCHAR(20)   NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    tratamiento VARCHAR(120)  COMMENT 'Texto libre que escribe la persona',
    mensaje     VARCHAR(1000),
    fecha       DATETIME(6)   NOT NULL,
    estado      VARCHAR(20)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_solicitudes_contacto_estado CHECK (estado IN ('NUEVO', 'CONTACTADO', 'CERRADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Mensajes del formulario de contacto de la landing';
