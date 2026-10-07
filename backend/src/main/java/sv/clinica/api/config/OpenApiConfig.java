package sv.clinica.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sv.clinica.api.dto.ErrorResponse;

/**
 * Documentación de la API (Swagger) en /swagger-ui.html y /v3/api-docs. Solo en dev y test: en prod está apagada.
 * Para probar los endpoints privados: POST /api/auth/login, copiar "token_acceso" y pegarlo en «Authorize».
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_ERROR = "ErrorResponse";

    @Bean
    OpenAPI documentacion() {
        return new OpenAPI()
                .info(new Info()
                        .title("AEOD · API central")
                        .version("1")
                        .description("API de la landing y del software de gestión. Los endpoints con candado necesitan "
                                + "el token de acceso de POST /api/auth/login (botón «Authorize»). Todos los errores "
                                + "tienen el mismo formato (ErrorResponse)."))
                .components(new Components().addSecuritySchemes("bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    /** Los esquemas usan los mismos nombres de campo que el JSON real (snake_case). */
    @Bean
    ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }

    /** Cada operación documenta su respuesta de error con el formato único de la API. */
    @Bean
    OpenApiCustomizer erroresDeLaApi() {
        return api -> {
            Schema<?> error = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class)).schema;
            api.getComponents().addSchemas(ESQUEMA_ERROR, error);
            // Los valores antes/después de la auditoría son JSON libre
            api.getComponents().addSchemas("JsonNode", new ObjectSchema().additionalProperties(true)
                    .description("Datos en formato JSON"));
            ApiResponse respuesta = new ApiResponse()
                    .description("Error (400 datos no válidos, 401 sin sesión, 403 sin permiso, 404 no existe, 409 conflicto, 429 demasiadas peticiones…)")
                    .content(new Content().addMediaType("application/json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_ERROR))));
            api.getPaths().values().forEach(ruta -> ruta.readOperations()
                    .forEach(operacion -> operacion.getResponses().addApiResponse("default", respuesta)));
        };
    }
}
