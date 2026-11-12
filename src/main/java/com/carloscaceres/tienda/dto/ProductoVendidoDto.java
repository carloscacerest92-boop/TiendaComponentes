package com.carloscaceres.tienda.dto;

import java.math.BigDecimal;

/** Ranking de ventas: unidades e ingresos agregados con SUM, sin contar pedidos cancelados. */
public record ProductoVendidoDto(String sku, String nombre, int unidadesVendidas, BigDecimal ingresos,
                                 int numeroPedidos) {
}
