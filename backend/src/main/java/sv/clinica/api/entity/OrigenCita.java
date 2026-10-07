package sv.clinica.api.entity;

/** Desde dónde se crea o modifica una cita. Todas acaban en la misma tabla. */
public enum OrigenCita {
    /** La web pública. */
    LANDING,
    /** El software de gestión (recepción, odontólogos, coordinación). */
    SOFTWARE,
    /** La futura app del paciente. */
    APP;

    /** Las acciones del propio paciente están sujetas a las reglas de antelación. */
    public boolean esPaciente() {
        return this != SOFTWARE;
    }
}
