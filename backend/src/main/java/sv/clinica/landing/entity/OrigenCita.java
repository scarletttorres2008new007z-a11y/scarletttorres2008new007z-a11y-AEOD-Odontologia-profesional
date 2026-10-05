package sv.clinica.landing.entity;

/** Desde dónde se crea o modifica una cita. Hoy solo existe LANDING. */
public enum OrigenCita {
    LANDING,
    /** Futuro software de gestión de la clínica (recepción, odontólogos). */
    CLINICA,
    /** Futura app del paciente. */
    APP_PACIENTE;

    /** Las acciones del propio paciente están sujetas a las reglas de antelación. */
    public boolean esPaciente() {
        return this != CLINICA;
    }
}
