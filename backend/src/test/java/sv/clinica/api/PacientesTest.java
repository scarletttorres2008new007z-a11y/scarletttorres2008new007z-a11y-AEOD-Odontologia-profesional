package sv.clinica.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pacientes: alta, búsqueda, edición y baja, con sus validaciones, permisos y auditoría. */
class PacientesTest extends PruebaIntegracion {

    /** Datos mínimos de un paciente: nombre, apellidos y teléfono. */
    private static Map<String, Object> paciente(String nombres, String apellidos, String telefono) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", nombres);
        datos.put("apellidos", apellidos);
        datos.put("telefono", telefono);
        return datos;
    }

    private static Map<String, Object> conDni(Map<String, Object> datos, String numero) {
        datos.put("tipo_documento", "DNI");
        datos.put("numero_documento", numero);
        return datos;
    }

    private JsonNode crear(String token, Map<String, Object> datos) throws Exception {
        return leer(mvc.perform(conJson(conToken(post("/api/pacientes"), token), datos))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    /**
     * Criterio de terminado de la Fase 2: recepción da de alta, busca y edita pacientes, y cada cambio queda
     * en la auditoría.
     */
    @Test
    void recepcionDaDeAltaBuscaYEditaPacientesYTodoQuedaEnLaAuditoria() throws Exception {
        crearUsuario("marta", "RECEPCION");
        String recepcion = entrar("marta");

        Map<String, Object> datos = conDni(paciente("  José   Luis ", "García Pérez", "+34 600 12 34 56"), "12.345.678-z");
        datos.put("fecha_nacimiento", "1990-03-12");
        datos.put("sexo", "HOMBRE");
        datos.put("email", "JL.Garcia@Example.com");
        datos.put("direccion", "Calle Mayor 1, 28013 Madrid");
        datos.put("contacto_emergencia_nombre", "Ana Pérez (madre)");
        datos.put("contacto_emergencia_telefono", "611 222 333");
        datos.put("observaciones", "Prefiere que le llamen por la tarde.");
        JsonNode creado = crear(recepcion, datos);
        long id = creado.get("id").asLong();
        String codigo = creado.get("codigo").asText();

        // Se guarda normalizado y con un código aleatorio que no se puede adivinar
        assertThat(codigo).matches("^[2-9BCDFGHJKLMNPQRSTVWXYZ]{6}$");
        assertThat(creado.get("nombres").asText()).isEqualTo("José Luis");
        assertThat(creado.get("numero_documento").asText()).isEqualTo("12345678Z");
        assertThat(creado.get("telefono").asText()).isEqualTo("+34600123456");
        assertThat(creado.get("email").asText()).isEqualTo("jl.garcia@example.com");
        assertThat(creado.get("edad").asInt()).isEqualTo(36);
        assertThat(creado.get("activo").asBoolean()).isTrue();

        // Se encuentra por cualquiera de sus datos, en cualquier orden, sin tildes y con o sin separadores
        for (String texto : new String[]{"garcia jose", "JOSÉ LUIS GARCÍA", "600 12 34", "600-123-456", "12345678-Z",
                codigo.toLowerCase(), "jl.garcia@example"}) {
            mvc.perform(conToken(get("/api/pacientes").param("texto", texto), recepcion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_elementos").value(1))
                    .andExpect(jsonPath("$.contenido[0].id").value(id));
        }
        mvc.perform(conToken(get("/api/pacientes").param("texto", "garcia lucia"), recepcion))
                .andExpect(jsonPath("$.total_elementos").value(0));

        // Edita el teléfono y borra el correo
        datos.put("telefono", "622 333 444");
        datos.put("email", "");
        mvc.perform(conJson(conToken(put("/api/pacientes/" + id), recepcion), datos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("622333444"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.codigo").value(codigo));

        // Lo da de baja: no se borra
        mvc.perform(conJson(conToken(put("/api/pacientes/" + id + "/estado"), recepcion), Map.of("activo", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(conToken(get("/api/pacientes/" + id), recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // Cada cambio queda en la auditoría, con quién lo hizo; la edición guarda solo lo que cambió
        crearUsuario("jefa", "ADMINISTRADOR");
        mvc.perform(conToken(get("/api/auditoria").param("entidad", "PACIENTE").param("entidad_id", String.valueOf(id)),
                        entrar("jefa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(3)))
                .andExpect(jsonPath("$.contenido[*].accion", contains("DESACTIVAR", "EDITAR", "CREAR")))
                .andExpect(jsonPath("$.contenido[*].usuario.username", contains("marta", "marta", "marta")))
                .andExpect(jsonPath("$.contenido[0].valor_nuevo.activo").value(false))
                .andExpect(jsonPath("$.contenido[1].valor_anterior.telefono").value("+34600123456"))
                .andExpect(jsonPath("$.contenido[1].valor_anterior.email").value("jl.garcia@example.com"))
                .andExpect(jsonPath("$.contenido[1].valor_nuevo.telefono").value("622333444"))
                .andExpect(jsonPath("$.contenido[1].valor_anterior.nombres").doesNotExist())
                .andExpect(jsonPath("$.contenido[2].valor_nuevo.codigo").value(codigo))
                .andExpect(jsonPath("$.contenido[2].valor_nuevo.numero_documento").value("12345678Z"));
    }

    @Test
    void elBackendValidaLosDatosAunqueElSoftwareNoLoHaga() throws Exception {
        crearUsuario("marta", "RECEPCION");
        String recepcion = entrar("marta");

        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), Map.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Revisa los datos enviados."))
                .andExpect(jsonPath("$.errores.nombres").value("El nombre es obligatorio."))
                .andExpect(jsonPath("$.errores.apellidos").value("Los apellidos son obligatorios."))
                .andExpect(jsonPath("$.errores.telefono").value("El teléfono es obligatorio."));

        Map<String, Object> malos = paciente("J0sé", "García", "123");
        malos.put("email", "no-es-un-correo");
        malos.put("contacto_emergencia_telefono", "12");
        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), malos))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombres").value("Usa solo letras, espacios, apóstrofos y guiones."))
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.contacto_emergencia_telefono").exists());

        // DNI y NIE con su letra de control
        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), conDni(paciente("Ana", "Martín", "600111222"), "12345678A")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numero_documento").value("La letra no corresponde a ese número de DNI. Revísalo."));
        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), conDni(paciente("Ana", "Martín", "600111222"), "1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numero_documento").value("El DNI son 8 números y una letra, por ejemplo 12345678Z."));
        Map<String, Object> conNie = paciente("Ana", "Martín", "600111222");
        conNie.put("tipo_documento", "NIE");
        conNie.put("numero_documento", "x-1234567-l");
        crear(recepcion, conNie);

        // Tipo y número van juntos
        Map<String, Object> soloTipo = paciente("Ana", "Martín", "600111222");
        soloTipo.put("tipo_documento", "PASAPORTE");
        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), soloTipo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numero_documento").value("Escribe el número del documento."));
        Map<String, Object> soloNumero = paciente("Ana", "Martín", "600111222");
        soloNumero.put("numero_documento", "AB123456");
        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), soloNumero))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipo_documento").value("Elige el tipo de documento."));
        Map<String, Object> tipoInventado = paciente("Ana", "Martín", "600111222");
        tipoInventado.put("tipo_documento", "CARNET");
        tipoInventado.put("numero_documento", "AB123456");
        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), tipoInventado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipo_documento").exists());

        // La fecha de nacimiento no puede ser futura (hoy es el 12/10/2026 en Madrid)
        Map<String, Object> futura = paciente("Ana", "Martín", "600111222");
        futura.put("fecha_nacimiento", "2026-10-13");
        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), futura))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha_nacimiento").value("La fecha de nacimiento no puede ser posterior a hoy."));

        // Solo se guardó el paciente con NIE válido
        assertThat(jdbc.queryForObject("select count(*) from pacientes", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select numero_documento from pacientes", String.class)).isEqualTo("X1234567L");
    }

    @Test
    void noAdmiteDosPacientesConElMismoDocumento() throws Exception {
        crearUsuario("marta", "RECEPCION");
        String recepcion = entrar("marta");
        crear(recepcion, conDni(paciente("Ana", "Martín", "600111222"), "12345678Z"));

        mvc.perform(conJson(conToken(post("/api/pacientes"), recepcion), conDni(paciente("Ana María", "Martín", "600111333"), "12345678 z")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.numero_documento")
                        .value("Ya hay un paciente con ese documento. Búscalo en la lista en lugar de darlo de alta otra vez."));

        long otro = crear(recepcion, paciente("Luis", "Martín", "600111444")).get("id").asLong();
        mvc.perform(conJson(conToken(put("/api/pacientes/" + otro), recepcion), conDni(paciente("Luis", "Martín", "600111444"), "12345678Z")))
                .andExpect(status().isConflict());
        // Un DNI de 7 cifras se guarda con el 0 delante
        mvc.perform(conJson(conToken(put("/api/pacientes/" + otro), recepcion), conDni(paciente("Luis", "Martín", "600111444"), "1234567L")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero_documento").value("01234567L"));
    }

    @Test
    void losPermisosSeCompruebanEnLaApi() throws Exception {
        crearUsuario("marta", "RECEPCION");
        long id = crear(entrar("marta"), paciente("Ana", "Martín", "600111222")).get("id").asLong();

        // De serie, el odontólogo consulta pacientes pero no los cambia
        crearUsuario("doctora", "ODONTOLOGO");
        String odontologo = entrar("doctora");
        mvc.perform(conToken(get("/api/pacientes"), odontologo)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/pacientes/" + id), odontologo)).andExpect(status().isOk());
        mvc.perform(conJson(conToken(post("/api/pacientes"), odontologo), paciente("Luis", "Ruiz", "600111333")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción."));
        mvc.perform(conJson(conToken(put("/api/pacientes/" + id), odontologo), paciente("Ana", "Otra", "600111222")))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(put("/api/pacientes/" + id + "/estado"), odontologo), Map.of("activo", false)))
                .andExpect(status().isForbidden());

        // Sin ningún permiso de pacientes no ve nada, y sin sesión tampoco
        crearUsuario("sinrol");
        mvc.perform(conToken(get("/api/pacientes"), entrar("sinrol"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/pacientes/" + id)).andExpect(status().isUnauthorized());

        // Nada de lo rechazado cambió los datos
        assertThat(jdbc.queryForObject("select count(*) from pacientes", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select apellidos from pacientes", String.class)).isEqualTo("Martín");
        assertThat(jdbc.queryForObject("select count(*) from auditoria where entidad = 'PACIENTE'", Integer.class)).isEqualTo(1);
    }

    @Test
    void listadoOrdenadoPaginadoYPorEstado() throws Exception {
        crearUsuario("marta", "RECEPCION");
        String recepcion = entrar("marta");
        crear(recepcion, paciente("Lucía", "Gómez", "600000001"));
        long ruiz = crear(recepcion, paciente("Álvaro", "Ruiz", "600000002")).get("id").asLong();
        crear(recepcion, paciente("Ana", "Martín", "600000003"));
        mvc.perform(conJson(conToken(put("/api/pacientes/" + ruiz + "/estado"), recepcion), Map.of("activo", false)))
                .andExpect(status().isOk());

        mvc.perform(conToken(get("/api/pacientes").param("tamano", "2"), recepcion))
                .andExpect(jsonPath("$.contenido[*].apellidos", contains("Gómez", "Martín")))
                .andExpect(jsonPath("$.total_elementos").value(3))
                .andExpect(jsonPath("$.total_paginas").value(2));
        mvc.perform(conToken(get("/api/pacientes").param("activo", "false"), recepcion))
                .andExpect(jsonPath("$.contenido[*].apellidos", contains("Ruiz")));
        mvc.perform(conToken(get("/api/pacientes").param("activo", "true"), recepcion))
                .andExpect(jsonPath("$.contenido[*].apellidos", contains("Gómez", "Martín")));
        mvc.perform(conToken(get("/api/pacientes").param("texto", "alvaro"), recepcion))
                .andExpect(jsonPath("$.contenido[*].nombres", contains("Álvaro")));
        // Los comodines de SQL se buscan tal cual
        mvc.perform(conToken(get("/api/pacientes").param("texto", "%"), recepcion))
                .andExpect(jsonPath("$.total_elementos").value(0));
    }

    @Test
    void guardarSinCambiosNoDejaRastroYUnPacienteInexistenteDa404() throws Exception {
        crearUsuario("marta", "RECEPCION");
        String recepcion = entrar("marta");
        Map<String, Object> datos = paciente("Ana", "Martín", "600 111 222");
        long id = crear(recepcion, datos).get("id").asLong();

        mvc.perform(conJson(conToken(put("/api/pacientes/" + id), recepcion), datos)).andExpect(status().isOk());
        mvc.perform(conJson(conToken(put("/api/pacientes/" + id + "/estado"), recepcion), Map.of("activo", true)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from auditoria where entidad = 'PACIENTE'", Integer.class)).isEqualTo(1);

        mvc.perform(conToken(get("/api/pacientes/999999"), recepcion))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El paciente no existe."));
        mvc.perform(conJson(conToken(put("/api/pacientes/999999"), recepcion), datos)).andExpect(status().isNotFound());
    }

    @Test
    void elCodigoNoSeRepiteNiSeEligeDesdeFuera() throws Exception {
        crearUsuario("marta", "RECEPCION");
        String recepcion = entrar("marta");
        Map<String, Object> datos = paciente("Ana", "Martín", "600111222");
        datos.put("codigo", "AAAAAA");
        datos.put("id", 5);
        datos.put("activo", false);
        JsonNode creado = crear(recepcion, datos);
        assertThat(creado.get("codigo").asText()).isNotEqualTo("AAAAAA").matches("^[2-9BCDFGHJKLMNPQRSTVWXYZ]{6}$");
        assertThat(creado.get("activo").asBoolean()).isTrue();

        for (int i = 0; i < 20; i++) crear(recepcion, paciente("Ana", "Martín", "600111222"));
        assertThat(jdbc.queryForObject("select count(distinct codigo) from pacientes", Integer.class)).isEqualTo(21);
        mvc.perform(conToken(get("/api/pacientes").param("tamano", "100"), recepcion))
                .andExpect(jsonPath("$.contenido[*].codigo", hasSize(21)))
                .andExpect(jsonPath("$.contenido[0].codigo", matchesPattern("^[2-9BCDFGHJKLMNPQRSTVWXYZ]{6}$")));
    }
}
