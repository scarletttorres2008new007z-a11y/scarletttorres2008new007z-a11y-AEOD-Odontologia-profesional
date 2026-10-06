package sv.clinica.api;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import sv.clinica.api.entity.Usuario;
import sv.clinica.api.security.CookieDeSesion;
import sv.clinica.api.security.TokenService;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Entrar, renovar, salir, bloqueo por intentos y cambio de contraseña. */
class SesionTest extends PruebaIntegracion {

    private static final String INCORRECTOS = "Usuario o contraseña incorrectos.";
    private static final String SESION_TERMINADA = "Tu sesión ha terminado. Vuelve a iniciar sesión.";

    @Autowired
    JwtEncoder encoder;

    @Test
    void entraConUsuarioOCorreoYRecibeTokenYCookie() throws Exception {
        crearUsuario("lucia", "RECEPCION");

        MvcResult resultado = mvc.perform(login("  LUCIA ", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expira_en").value(900))
                .andExpect(jsonPath("$.token_acceso").isString())
                .andExpect(jsonPath("$.usuario.username").value("lucia"))
                .andExpect(jsonPath("$.usuario.roles[0].codigo").value("RECEPCION"))
                .andExpect(jsonPath("$.usuario.permisos", contains("bloqueos.gestionar", "citas.cambiar_estado", "citas.cancelar",
                        "citas.crear", "citas.editar", "citas.reprogramar", "citas.ver", "citas.ver_todas",
                        "pacientes.crear", "pacientes.editar", "pacientes.ver")))
                .andExpect(jsonPath("$.usuario.password_hash").doesNotExist())
                .andReturn();

        String cookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith(CookieDeSesion.NOMBRE + "=")
                .contains("Path=/api/auth", "HttpOnly", "SameSite=Strict", "Secure", "Max-Age=43200");
        // En la base solo queda la huella del token de refresco, nunca el token
        String token = resultado.getResponse().getCookie(CookieDeSesion.NOMBRE).getValue();
        assertThat(jdbc.queryForObject("select refresh_token_hash from sesiones", String.class))
                .hasSize(64).isNotEqualTo(token);
        assertThat(jdbc.queryForObject("select count(*) from auditoria where accion = 'INICIAR_SESION'", Integer.class))
                .isEqualTo(1);

        mvc.perform(login("lucia@example.com", CLAVE)).andExpect(status().isOk());
    }

    @Test
    void mismosErroresParaUsuarioDesconocidoYContrasenaIncorrecta() throws Exception {
        crearUsuario("lucia", "RECEPCION");
        mvc.perform(login("nadie", CLAVE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(INCORRECTOS))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
        mvc.perform(login("lucia", "otra-clave-cualquiera"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(INCORRECTOS));
        mvc.perform(login("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.usuario").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    /** BCrypt solo mira 72 bytes: la contraseña buena seguida de más caracteres no puede servir. */
    @Test
    void unaContrasenaDeMasDe72BytesNuncaEntra() throws Exception {
        Usuario larga = crearUsuario("larga", "RECEPCION");
        String password72 = "a".repeat(72);
        larga.cambiarPassword(passwords.encode(password72), AHORA);
        usuarios.save(larga);

        mvc.perform(login("larga", password72 + "loquesea")).andExpect(status().isUnauthorized());
        mvc.perform(login("larga", password72)).andExpect(status().isOk());
    }

    @Test
    void cincoFallosSeguidosBloqueanLaCuenta() throws Exception {
        crearUsuario("lucia", "RECEPCION");
        for (int i = 0; i < 4; i++) {
            mvc.perform(login("lucia", "incorrecta-" + i)).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("lucia", "incorrecta-5"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("LOCKED"))
                .andExpect(jsonPath("$.message").value(
                        "Demasiados intentos fallidos. Por seguridad, la cuenta queda bloqueada 15 minutos."));
        // Ni siquiera con la contraseña buena, hasta que pase el bloqueo
        mvc.perform(login("lucia", CLAVE)).andExpect(status().isLocked());

        assertThat(jdbc.queryForObject("select count(*) from auditoria where accion = 'INICIO_SESION_FALLIDO'", Integer.class))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from auditoria where accion = 'BLOQUEAR_POR_INTENTOS'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select bloqueado_hasta from usuarios where username = 'lucia'", String.class))
                .startsWith("2026-10-12 08:15:00");
    }

    @Test
    void unUsuarioDesactivadoNoEntra() throws Exception {
        Usuario baja = crearUsuario("baja", "RECEPCION");
        baja.cambiarEstado(false, AHORA);
        usuarios.save(baja);

        mvc.perform(login("baja", "incorrecta"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(INCORRECTOS));
        mvc.perform(login("baja", CLAVE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tu usuario está desactivado. Habla con el administrador de la clínica."));
    }

    @Test
    void sinTokenOConTokenNoValidoResponde401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.message").value("Inicia sesión para continuar."))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
        mvc.perform(conToken(get("/api/auth/me"), "no-es-un-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESION_TERMINADA));

        Usuario lucia = crearUsuario("lucia", "RECEPCION");
        String token = entrar("lucia");
        Long sesion = jdbc.queryForObject("select id from sesiones", Long.class);

        // Firmado con otra clave
        NimbusJwtEncoder otraClave = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec("otra-clave-de-32-bytes-o-mas-123456".getBytes(), "HmacSHA256")));
        mvc.perform(conToken(get("/api/auth/me"), firmar(otraClave, lucia.getId(), sesion, AHORA_INSTANTE, 900)))
                .andExpect(status().isUnauthorized());
        // Caducado
        mvc.perform(conToken(get("/api/auth/me"), firmar(encoder, lucia.getId(), sesion, AHORA_INSTANTE.minusSeconds(1200), 900)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESION_TERMINADA));
        // De otro usuario con la sesión de lucía
        mvc.perform(conToken(get("/api/auth/me"), firmar(encoder, lucia.getId() + 1000, sesion, AHORA_INSTANTE, 900)))
                .andExpect(status().isUnauthorized());
        // El bueno
        mvc.perform(conToken(get("/api/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("lucia"));
    }

    @Test
    void renovarExigeLaCookieYLaCabeceraDelSoftware() throws Exception {
        crearUsuario("lucia", "RECEPCION");
        Cookie cookie = mvc.perform(login("lucia", CLAVE)).andReturn().getResponse().getCookie(CookieDeSesion.NOMBRE);

        mvc.perform(refresh().cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_acceso").isString())
                .andExpect(jsonPath("$.usuario.username").value("lucia"));
        // Sin la cabecera X-Requested-With (por ejemplo, un formulario de otra web)
        mvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isForbidden());
        // Sin cookie o con una cookie inventada: 401 y la cookie se borra
        mvc.perform(refresh())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESION_TERMINADA));
        mvc.perform(refresh().cookie(new Cookie(CookieDeSesion.NOMBRE, "inventada")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void cerrarSesionInvalidaElTokenAlMomento() throws Exception {
        crearUsuario("lucia", "RECEPCION");
        MvcResult entrada = mvc.perform(login("lucia", CLAVE)).andReturn();
        String token = leer(entrada.getResponse().getContentAsString()).get("token_acceso").asText();
        Cookie cookie = entrada.getResponse().getCookie(CookieDeSesion.NOMBRE);

        mvc.perform(post("/api/auth/logout").cookie(cookie).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mvc.perform(conToken(get("/api/auth/me"), token)).andExpect(status().isUnauthorized());
        mvc.perform(refresh().cookie(cookie)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from auditoria where accion = 'CERRAR_SESION'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void cambiarLaContrasenaCierraLasOtrasSesiones() throws Exception {
        crearUsuario("lucia", "RECEPCION");
        String otroEquipo = entrar("lucia");
        String esteEquipo = entrar("lucia");
        String nueva = "Nueva-clave-segura-1";

        mvc.perform(conJson(conToken(put("/api/auth/password"), esteEquipo),
                        Map.of("password_actual", "no-es-la-actual", "password_nueva", nueva)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password_actual").value("La contraseña actual no es correcta."));
        mvc.perform(conJson(conToken(put("/api/auth/password"), esteEquipo),
                        Map.of("password_actual", CLAVE, "password_nueva", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password_nueva").value("La contraseña debe tener al menos 10 caracteres."));
        mvc.perform(conJson(conToken(put("/api/auth/password"), esteEquipo),
                        Map.of("password_actual", CLAVE, "password_nueva", "lucia@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password_nueva").value("La contraseña no puede ser igual al usuario ni al correo."));

        mvc.perform(conJson(conToken(put("/api/auth/password"), esteEquipo),
                        Map.of("password_actual", CLAVE, "password_nueva", nueva)))
                .andExpect(status().isNoContent());

        mvc.perform(conToken(get("/api/auth/me"), esteEquipo)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/auth/me"), otroEquipo)).andExpect(status().isUnauthorized());
        mvc.perform(login("lucia", CLAVE)).andExpect(status().isUnauthorized());
        mvc.perform(login("lucia", nueva)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select password_hash from usuarios where username = 'lucia'", String.class))
                .startsWith("$2a$").doesNotContain(nueva);
    }

    private static final Instant AHORA_INSTANTE = AHORA.atZone(MADRID).toInstant();

    private static MockHttpServletRequestBuilder refresh() {
        return post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest");
    }

    private static String firmar(JwtEncoder encoder, Long usuarioId, Long sesionId, Instant emitido, long segundos) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(TokenService.EMISOR)
                .subject(String.valueOf(usuarioId))
                .claim(TokenService.CLAIM_SESION, sesionId)
                .issuedAt(emitido)
                .expiresAt(emitido.plusSeconds(segundos))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
