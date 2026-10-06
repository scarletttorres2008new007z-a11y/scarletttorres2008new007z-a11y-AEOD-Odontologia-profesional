package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta al iniciar o renovar la sesión. El token de acceso va en la cabecera
 * "Authorization: Bearer …" de cada petición; el de refresco viaja solo en una cookie HttpOnly.
 */
@Schema(description = "Sesión iniciada", requiredProperties = {"token_acceso", "tipo", "expira_en", "usuario"})
public record SesionResponse(
        @Schema(description = "Token de acceso (JWT) de corta duración") String tokenAcceso,
        @Schema(example = "Bearer") String tipo,
        @Schema(description = "Segundos que dura el token de acceso", example = "900") long expiraEn,
        UsuarioActualResponse usuario) {
}
