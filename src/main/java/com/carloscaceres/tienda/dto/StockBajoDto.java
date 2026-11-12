package com.carloscaceres.tienda.dto;

/** Producto que necesita reposición. unidadesAReponer se calcula en la propia consulta. */
public record StockBajoDto(String sku, String nombre, int stock, int stockMinimo, int unidadesAReponer,
                           int ultimaEntrada) {
}
