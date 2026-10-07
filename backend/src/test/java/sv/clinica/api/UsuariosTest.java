package sv.clinica.api;

import org.junit.jupiter.api.Test;
import sv.clinica.api.entity.Usuario;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usuarios, roles, permisos y auditoría: lo que el backend permite y rechaza según los permisos. */
class UsuariosTest extends PruebaIntegracion {

    private static final String SIN_PERMISO = "No tienes permiso para realizar esta acción.";

    /**
     * Criterio de terminado de la Fase 1: un administrador entra, crea un usuario de recepción y la API rechaza
     * con 403 lo que ese usuario no tiene permitido, aunque llame directamente a los endpoints.
     */
    @Test
    void elAdministradorCreaRecepcionYLaApiLeRechazaLoQueNoTienePermitido() throws Exception {
        crearUsuario("jefa", "ADMINISTRADOR");
        String admin = entrar("jefa");

        String creado = mvc.perform(conJson(conToken(post("/api/usuarios"), admin), Map.of(
                        "username", "Recepcion1", "email", "Recepcion1@Example.com", "nombre", "  Marta Ruiz ",
                        "password", "Recepcion-2026", "roles", List.of("RECEPCION"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("recepcion1"))
                .andExpect(jsonPath("$.email").value("recepcion1@example.com"))
                .andExpect(jsonPath("$.nombre").value("Marta Ruiz"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.roles[0].codigo").value("RECEPCION"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = leer(creado).get("id").asLong();

        // Recepción solo trae de serie los permisos de pacientes y de la agenda
        String recepcion = leer(mvc.perform(login("recepcion1", "Recepcion-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.permisos", contains("bloqueos.gestionar", "citas.cambiar_estado", "citas.cancelar",
                        "citas.crear", "citas.editar", "citas.reprogramar", "citas.ver", "citas.ver_todas",
                        "pacientes.crear", "pacientes.editar", "pacientes.ver")))
                .andReturn().getResponse().getContentAsString()).get("token_acceso").asText();

        // Todo lo que no tiene permitido, llamando directamente a la API
        mvc.perform(conToken(get("/api/usuarios"), recepcion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(SIN_PERMISO))
                .andExpect(jsonPath("$.path").value("/api/usuarios"));
        mvc.perform(conToken(get("/api/usuarios/" + id), recepcion)).andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(post("/api/usuarios"), recepcion), Map.of(
                        "username", "intruso", "email", "intruso@example.com", "nombre", "Intruso", "password", "Intruso-2026")))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(put("/api/usuarios/" + id + "/roles"), recepcion), Map.of("roles", List.of("ADMINISTRADOR"))))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(put("/api/usuarios/" + id + "/estado"), recepcion), Map.of("activo", false)))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/roles"), recepcion)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/permisos"), recepcion)).andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(put("/api/roles/2/permisos"), recepcion), Map.of("permisos", List.of("usuarios.ver"))))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/auditoria"), recepcion)).andExpect(status().isForbidden());
        // Lo que sí puede: ver su propia cuenta
        mvc.perform(conToken(get("/api/auth/me"), recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("recepcion1"));

        // Nada de lo rechazado cambió los datos
        assertThat(usuarios.findByUsername("intruso")).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from usuario_roles where usuario_id = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void unPermisoNuevoSeAplicaAlMomentoSinVolverAEntrar() throws Exception {
        crearUsuario("jefa", "ADMINISTRADOR");
        crearUsuario("marta", "RECEPCION");
        String admin = entrar("jefa");
        String recepcion = entrar("marta");
        mvc.perform(conToken(get("/api/usuarios"), recepcion)).andExpect(status().isForbidden());

        long rolRecepcion = roles.findByCodigo("RECEPCION").orElseThrow().getId();
        mvc.perform(conJson(conToken(put("/api/roles/" + rolRecepcion + "/permisos"), admin),
                        Map.of("permisos", List.of("usuarios.ver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permisos[0]").value("usuarios.ver"))
                .andExpect(jsonPath("$.usuarios").value(1));

        mvc.perform(conToken(get("/api/usuarios"), recepcion)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/auth/me"), recepcion)).andExpect(jsonPath("$.permisos[0]").value("usuarios.ver"));
        mvc.perform(conJson(conToken(post("/api/usuarios"), recepcion), Map.of(
                        "username", "intruso", "email", "intruso@example.com", "nombre", "Intruso", "password", "Intruso-2026")))
                .andExpect(status().isForbidden());
    }

    @Test
    void desactivarCortaElAccesoAlMomento() throws Exception {
        crearUsuario("jefa", "ADMINISTRADOR");
        Usuario marta = crearUsuario("marta", "RECEPCION");
        String admin = entrar("jefa");
        String recepcion = entrar("marta");

        mvc.perform(conJson(conToken(put("/api/usuarios/" + marta.getId() + "/estado"), admin), Map.of("activo", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(conToken(get("/api/auth/me"), recepcion)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from sesiones where usuario_id = ? and revocada_en is null",
                Integer.class, marta.getId())).isZero();

        mvc.perform(conJson(conToken(put("/api/usuarios/" + marta.getId() + "/estado"), admin), Map.of("activo", true)))
                .andExpect(status().isOk());
        assertThat(entrar("marta")).isNotBlank();
    }

    @Test
    void reglasQueProtegenAlAdministrador() throws Exception {
        Usuario jefa = crearUsuario("jefa", "ADMINISTRADOR");
        String admin = entrar("jefa");

        mvc.perform(conJson(conToken(put("/api/usuarios/" + jefa.getId() + "/estado"), admin), Map.of("activo", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No puedes desactivar tu propio usuario."));
        mvc.perform(conJson(conToken(put("/api/usuarios/" + jefa.getId() + "/roles"), admin), Map.of("roles", List.of("RECEPCION"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.roles").value("No puedes quitarte a ti mismo el rol de administrador."));
        mvc.perform(conJson(conToken(put("/api/usuarios/" + jefa.getId() + "/password"), admin), Map.of("password", "Otra-clave-2026")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Para cambiar tu propia contraseña usa «Mi cuenta»."));
        long rolAdministrador = roles.findByCodigo("ADMINISTRADOR").orElseThrow().getId();
        mvc.perform(conJson(conToken(put("/api/roles/" + rolAdministrador + "/permisos"), admin), Map.of("permisos", List.of())))
                .andExpect(status().isConflict());

        // Un coordinador que gestiona usuarios no puede tocar a un administrador ni hacerse administrador
        Usuario coordinador = crearUsuario("coordina", "COORDINADOR");
        darPermisos("COORDINADOR", "usuarios.ver", "usuarios.crear", "usuarios.editar", "usuarios.asignar_roles");
        String coordina = entrar("coordina");
        mvc.perform(conJson(conToken(put("/api/usuarios/" + jefa.getId() + "/password"), coordina), Map.of("password", "Robada-clave-2026")))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(put("/api/usuarios/" + jefa.getId() + "/estado"), coordina), Map.of("activo", false)))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(put("/api/usuarios/" + coordinador.getId() + "/roles"), coordina),
                        Map.of("roles", List.of("COORDINADOR", "ADMINISTRADOR"))))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(post("/api/usuarios"), coordina), Map.of("username", "nuevo", "email", "nuevo@example.com",
                        "nombre", "Nuevo", "password", "Nuevo-clave-2026", "roles", List.of("ADMINISTRADOR"))))
                .andExpect(status().isForbidden());
        // Sí puede crear personal sin rol de administrador
        mvc.perform(conJson(conToken(post("/api/usuarios"), coordina), Map.of("username", "nuevo", "email", "nuevo@example.com",
                        "nombre", "Nuevo", "password", "Nuevo-clave-2026", "roles", List.of("RECEPCION"))))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from usuarios u join usuario_roles ur on ur.usuario_id = u.id "
                + "join roles r on r.id = ur.rol_id where r.codigo = 'ADMINISTRADOR' and u.activo = 1", Integer.class)).isEqualTo(2);
    }

    @Test
    void crearSinPermisoParaAsignarRoles() throws Exception {
        crearUsuario("coordina", "COORDINADOR");
        darPermisos("COORDINADOR", "usuarios.crear");
        String coordina = entrar("coordina");

        mvc.perform(conJson(conToken(post("/api/usuarios"), coordina), Map.of("username", "nuevo", "email", "nuevo@example.com",
                        "nombre", "Nuevo", "password", "Nuevo-clave-2026", "roles", List.of("RECEPCION"))))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(conToken(post("/api/usuarios"), coordina), Map.of("username", "nuevo", "email", "nuevo@example.com",
                        "nombre", "Nuevo", "password", "Nuevo-clave-2026")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", hasSize(0)));
    }

    @Test
    void validaLosDatosDelUsuario() throws Exception {
        crearUsuario("jefa", "ADMINISTRADOR");
        crearUsuario("marta", "RECEPCION");
        String admin = entrar("jefa");

        mvc.perform(conJson(conToken(post("/api/usuarios"), admin), Map.of(
                        "username", "a b", "email", "no-es-correo", "nombre", "", "password", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.username").exists())
                .andExpect(jsonPath("$.errores.email").value("Escribe un correo válido."))
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio."))
                .andExpect(jsonPath("$.errores.password").value("La contraseña es obligatoria."));
        mvc.perform(conJson(conToken(post("/api/usuarios"), admin), Map.of(
                        "username", "MARTA", "email", "otra@example.com", "nombre", "Otra", "password", "Otra-clave-2026")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.username").value("Ya existe un usuario con ese nombre de usuario."));
        mvc.perform(conJson(conToken(post("/api/usuarios"), admin), Map.of(
                        "username", "otra", "email", "MARTA@example.com", "nombre", "Otra", "password", "Otra-clave-2026")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.email").value("Ya existe un usuario con ese correo."));
        mvc.perform(conJson(conToken(post("/api/usuarios"), admin), Map.of(
                        "username", "otra", "email", "otra@example.com", "nombre", "Otra", "password", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").value("La contraseña debe tener al menos 10 caracteres."));
        mvc.perform(conJson(conToken(post("/api/usuarios"), admin), Map.of(
                        "username", "otra", "email", "otra@example.com", "nombre", "Otra", "password", "Otra-clave-2026",
                        "roles", List.of("INVENTADO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.roles").value("Alguno de los roles elegidos no existe."));
        mvc.perform(conToken(get("/api/usuarios/999999"), admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El usuario no existe."));
    }

    @Test
    void buscarYPaginar() throws Exception {
        crearUsuario("jefa", "ADMINISTRADOR");
        crearUsuario("marta_ruiz", "RECEPCION");
        crearUsuario("martaxruiz", "RECEPCION");
        Usuario baja = crearUsuario("baja", "ODONTOLOGO");
        baja.cambiarEstado(false, AHORA);
        usuarios.save(baja);
        String admin = entrar("jefa");

        // El guion bajo se busca tal cual, no como comodín
        mvc.perform(conToken(get("/api/usuarios").param("texto", "marta_"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elementos").value(1))
                .andExpect(jsonPath("$.contenido[0].username").value("marta_ruiz"));
        mvc.perform(conToken(get("/api/usuarios").param("activo", "false"), admin))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].username").value("baja"));
        mvc.perform(conToken(get("/api/usuarios").param("tamano", "2").param("pagina", "1"), admin))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(2))
                .andExpect(jsonPath("$.total_elementos").value(5))
                .andExpect(jsonPath("$.total_paginas").value(3))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void restablecerLaContrasenaDeOtroUsuario() throws Exception {
        crearUsuario("jefa", "ADMINISTRADOR");
        Usuario marta = crearUsuario("marta", "RECEPCION");
        String admin = entrar("jefa");
        String sesionDeMarta = entrar("marta");

        mvc.perform(conJson(conToken(put("/api/usuarios/" + marta.getId() + "/password"), admin), Map.of("password", "marta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
        mvc.perform(conJson(conToken(put("/api/usuarios/" + marta.getId() + "/password"), admin), Map.of("password", "Nueva-de-marta-1")))
                .andExpect(status().isNoContent());

        mvc.perform(conToken(get("/api/auth/me"), sesionDeMarta)).andExpect(status().isUnauthorized());
        mvc.perform(login("marta", "Nueva-de-marta-1")).andExpect(status().isOk());
    }

    @Test
    void laAuditoriaGuardaQuienQueYCuandoSinContrasenas() throws Exception {
        Usuario jefa = crearUsuario("jefa", "ADMINISTRADOR");
        String admin = entrar("jefa");
        long id = leer(mvc.perform(conJson(conToken(post("/api/usuarios"), admin), Map.of("username", "marta",
                        "email", "marta@example.com", "nombre", "Marta", "password", "Marta-clave-2026", "roles", List.of("RECEPCION"))))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(conJson(conToken(put("/api/usuarios/" + id), admin),
                        Map.of("username", "marta", "email", "marta@example.com", "nombre", "Marta Ruiz")))
                .andExpect(status().isOk());
        mvc.perform(conJson(conToken(put("/api/usuarios/" + id + "/roles"), admin), Map.of("roles", List.of("RECEPCION", "COORDINADOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasSize(2)));

        mvc.perform(conToken(get("/api/auditoria").param("entidad", "USUARIO").param("entidad_id", String.valueOf(id)), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elementos").value(3))
                .andExpect(jsonPath("$.contenido[0].accion").value("CAMBIAR_ROLES"))
                .andExpect(jsonPath("$.contenido[0].valor_anterior.roles[0]").value("RECEPCION"))
                .andExpect(jsonPath("$.contenido[0].valor_nuevo.roles", hasSize(2)))
                .andExpect(jsonPath("$.contenido[1].accion").value("EDITAR"))
                .andExpect(jsonPath("$.contenido[1].valor_anterior.nombre").value("Marta"))
                .andExpect(jsonPath("$.contenido[1].valor_nuevo.nombre").value("Marta Ruiz"))
                .andExpect(jsonPath("$.contenido[2].accion").value("CREAR"))
                .andExpect(jsonPath("$.contenido[2].origen").value("SOFTWARE"))
                .andExpect(jsonPath("$.contenido[2].usuario.username").value("jefa"))
                .andExpect(jsonPath("$.contenido[2].ip").value("127.0.0.1"))
                .andExpect(jsonPath("$.contenido[2].creado_en").value("2026-10-12T08:00:00"));
        mvc.perform(conToken(get("/api/auditoria").param("usuario_id", String.valueOf(jefa.getId()))
                        .param("accion", "INICIAR_SESION").param("desde", "2026-10-12").param("hasta", "2026-10-12"), admin))
                .andExpect(jsonPath("$.total_elementos").value(1));
        mvc.perform(conToken(get("/api/auditoria").param("desde", "2026-10-13").param("hasta", "2026-10-12"), admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").exists());
        mvc.perform(conToken(get("/api/auditoria").param("accion", "INVENTADA"), admin))
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForList("select concat(coalesce(valor_anterior, ''), coalesce(valor_nuevo, '')) from auditoria", String.class))
                .noneMatch(valor -> valor.contains("password") || valor.contains("$2a$"));
    }
}
