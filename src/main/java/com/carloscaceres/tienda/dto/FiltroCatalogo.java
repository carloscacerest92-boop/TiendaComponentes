package com.carloscaceres.tienda.dto;

import com.carloscaceres.tienda.modelo.TipoProducto;

import java.math.BigDecimal;

/** Criterios opcionales de búsqueda en el catálogo. Un valor null significa "sin filtrar por esto". */
public record FiltroCatalogo(String categoria, TipoProducto tipo, BigDecimal precioMinimo,
                             BigDecimal precioMaximo, boolean soloDisponibles) {

    public static FiltroCatalogo sinFiltros() {
        return new FiltroCatalogo(null, null, null, null, false);
    }
}
