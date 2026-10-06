package sv.clinica.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados. "pagina" empieza en 0. */
@Schema(description = "Página de resultados",
        requiredProperties = {"contenido", "pagina", "tamano", "total_elementos", "total_paginas"})
public record PaginaResponse<T>(
        List<T> contenido,
        @Schema(description = "Número de página, empezando en 0") int pagina,
        @Schema(description = "Resultados por página") int tamano,
        long totalElementos,
        int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> convertir) {
        return new PaginaResponse<>(pagina.getContent().stream().map(convertir).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
