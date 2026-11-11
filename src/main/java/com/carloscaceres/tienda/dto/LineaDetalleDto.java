package com.carloscaceres.tienda.dto;

import java.math.BigDecimal;

/** Una línea del detalle de pedido, con datos del producto obtenidos por JOIN. */
public record LineaDetalleDto(String sku, String producto, int cantidad, BigDecimal precioUnitario,
                              BigDecimal importe) {
}
