package com.carloscaceres.tienda.dto;

import java.util.List;

/** Una página de resultados más los datos necesarios para navegar entre páginas. */
public record Pagina<T>(List<T> contenido, int numeroPagina, int tamanoPagina, int totalElementos) {

    public int totalPaginas() {
        return (totalElementos + tamanoPagina - 1) / tamanoPagina;
    }
}
