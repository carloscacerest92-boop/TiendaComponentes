package com.carloscaceres.tienda.dto;

import com.carloscaceres.tienda.modelo.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO: no representa una tabla, sino el resultado de una consulta que combina
 * pedido, cliente, dirección, líneas, productos y pago.
 */
public record DetallePedidoDto(int pedidoId, String cliente, String email, String direccionEnvio,
                               LocalDateTime fecha, EstadoPedido estado, BigDecimal total,
                               String estadoPago, List<LineaDetalleDto> lineas) {
}
