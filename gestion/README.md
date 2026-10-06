# AEOD · Software de gestión

Aplicación privada para el personal de la clínica (recepción, odontólogos, coordinación y administración).

- **Tecnología:** React + TypeScript + Vite, con CSS propio.
- **Datos:** solo a través de la API central (`../backend`), con login (Spring Security + JWT). Nunca se conecta a la base de datos.
- **Estado:** se crea en la **Fase 1** (login, estructura con menú lateral y cabecera, usuarios y roles). Los módulos
  siguientes llegan fase a fase: pacientes, agenda y citas, área clínica, administración, comunicación y reportes.
