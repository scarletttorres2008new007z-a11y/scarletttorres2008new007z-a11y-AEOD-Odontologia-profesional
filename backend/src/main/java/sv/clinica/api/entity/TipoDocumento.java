package sv.clinica.api.entity;

/** Documento de identidad del paciente. DNI y NIE se comprueban con su letra de control. */
public enum TipoDocumento {
    DNI,
    NIE,
    PASAPORTE,
    OTRO
}
