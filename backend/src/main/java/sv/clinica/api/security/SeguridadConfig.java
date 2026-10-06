package sv.clinica.api.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sv.clinica.api.config.SeguridadProperties;
import sv.clinica.api.exception.RespuestasDeError;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Set;

/**
 * Quién puede llamar a qué.
 *
 * - Lo que usa la landing (tratamientos, equipo, disponibilidad, reservar, contacto) es público, con CORS
 *   solo para los orígenes de CORS_ALLOWED_ORIGINS y límite de peticiones por IP.
 * - Todo lo demás exige un token de acceso válido de una sesión abierta. Cada endpoint del software comprueba
 *   además un permiso concreto (@PreAuthorize): aunque un botón no aparezca en pantalla, la API rechaza la
 *   operación con 403 si el usuario no tiene ese permiso.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    private static final Logger log = LoggerFactory.getLogger(SeguridadConfig.class);

    /** Lo que la landing lee sin sesión. */
    private static final String[] LECTURA_PUBLICA = {
            "/api/health", "/api/tratamientos/**", "/api/odontologos/**", "/api/disponibilidad/**"};
    /** Lo que la landing envía sin sesión. */
    private static final String[] ENVIO_PUBLICO = {"/api/citas", "/api/contacto"};
    /** Iniciar, renovar y cerrar sesión: se identifican con la contraseña o con la cookie de sesión, no con el token. */
    private static final Set<String> RUTAS_DE_SESION = Set.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout");
    /** Documentación de la API (Swagger). En producción está desactivada y estas rutas no existen. */
    private static final String[] DOCUMENTACION = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"};

    private static final int BYTES_MINIMOS_CLAVE = 32;

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ConvertidorJwt convertidor, RespuestasDeSeguridad respuestas,
                                          LimitadorDePeticiones limitador, RespuestasDeError errores,
                                          @Value("${app.cors.origenes}") List<String> origenesLanding) throws Exception {
        http
                // Sin CSRF de Spring: la API no se autentica con cookies sino con el token en la cabecera Authorization,
                // que otra web no puede añadir. La única cookie (la de sesión) es SameSite=Strict, solo viaja a
                // /api/auth y renovar o cerrar sesión exige además la cabecera X-Requested-With.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsDeLaLanding(origenesLanding)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(HttpMethod.GET, LECTURA_PUBLICA).permitAll()
                        .requestMatchers(HttpMethod.POST, ENVIO_PUBLICO).permitAll()
                        .requestMatchers(HttpMethod.POST, RUTAS_DE_SESION.toArray(String[]::new)).permitAll()
                        .requestMatchers(HttpMethod.GET, DOCUMENTACION).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(resolverDeToken())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .headers(h -> h
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        // Las respuestas de la API son datos, nunca páginas: el navegador no debe ejecutar ni cargar nada.
                        // (Swagger sí es una página con sus scripts, por eso esta regla solo va en /api/**.)
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                                new StaticHeadersWriter("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))))
                .addFilterBefore(new FiltroLimiteDePeticiones(limitador, errores), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS solo para lo que usa la landing (otro origen). El software de gestión se sirve desde el mismo origen
     * que la API (en desarrollo, a través del proxy de Vite), así que los endpoints privados no aceptan
     * llamadas desde ninguna otra web.
     */
    private static CorsConfigurationSource corsDeLaLanding(List<String> origenes) {
        CorsConfiguration landing = new CorsConfiguration();
        landing.setAllowedOrigins(origenes.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        landing.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        landing.setAllowedHeaders(List.of("Content-Type", "Accept"));
        landing.setAllowCredentials(false);
        landing.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        for (String ruta : LECTURA_PUBLICA) fuente.registerCorsConfiguration(ruta, landing);
        for (String ruta : ENVIO_PUBLICO) fuente.registerCorsConfiguration(ruta, landing);
        return fuente;
    }

    /** El token se lee de "Authorization: Bearer …", salvo al iniciar, renovar o cerrar sesión. */
    private static BearerTokenResolver resolverDeToken() {
        DefaultBearerTokenResolver cabecera = new DefaultBearerTokenResolver();
        return request -> RUTAS_DE_SESION.contains(request.getRequestURI()) ? null : cabecera.resolve(request);
    }

    @Bean
    PasswordEncoder passwordEncoder(SeguridadProperties seguridad) {
        return new BCryptPasswordEncoder(seguridad.costeBcrypt());
    }

    /**
     * Clave con la que se firman y comprueban los tokens (HS256). Sale de JWT_SECRET; nunca está en el código.
     * Solo en desarrollo y en las pruebas automáticas, sin JWT_SECRET se genera una clave aleatoria en cada arranque.
     */
    @Bean
    SecretKey claveDeTokens(SeguridadProperties seguridad) {
        String secreto = seguridad.jwtSecret();
        if (secreto == null || secreto.isBlank()) {
            if (!seguridad.claveTemporal()) {
                throw new IllegalStateException("JWT_SECRET está vacía. Define una clave aleatoria de al menos "
                        + BYTES_MINIMOS_CLAVE + " caracteres (ver backend/README.md, apartado «Entornos»).");
            }
            log.info("JWT_SECRET no está definida: se usa una clave temporal. Al reiniciar el backend, el software "
                    + "renueva la sesión solo. Esto solo se permite en tu equipo.");
            byte[] aleatoria = new byte[BYTES_MINIMOS_CLAVE];
            new SecureRandom().nextBytes(aleatoria);
            return new SecretKeySpec(aleatoria, "HmacSHA256");
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS_CLAVE) {
            throw new IllegalStateException("JWT_SECRET es demasiado corta: debe tener al menos "
                    + BYTES_MINIMOS_CLAVE + " caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    /** Comprueba firma, caducidad (con el reloj de la aplicación) y emisor. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey clave, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator caducidad = new JwtTimestampValidator();
        caducidad.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(caducidad, new JwtIssuerValidator(TokenService.EMISOR)));
        return decoder;
    }
}
