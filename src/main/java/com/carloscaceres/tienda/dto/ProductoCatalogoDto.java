package com.carloscaceres.tienda.dto;

import com.carloscaceres.tienda.modelo.TipoProducto;

import java.math.BigDecimal;

/** Fila del catálogo: producto + fabricante + socket + stock, obtenidos con JOIN. */
public record ProductoCatalogoDto(int id, String sku, String nombre, TipoProducto tipo, BigDecimal precio,
                                  String fabricante, String socket, int stock, boolean controlaStock) {

    public boolean disponible() {
        return !controlaStock || stock > 0;
    }
}
