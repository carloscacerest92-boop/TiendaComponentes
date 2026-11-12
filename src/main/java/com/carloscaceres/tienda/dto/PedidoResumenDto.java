package com.carloscaceres.tienda.dto;

import com.carloscaceres.tienda.modelo.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una fila del historial de pedidos de un cliente. */
public record PedidoResumenDto(int pedidoId, LocalDateTime fecha, EstadoPedido estado, int productosDistintos,
                               int unidades, BigDecimal total, String ciudadEnvio) {
}
