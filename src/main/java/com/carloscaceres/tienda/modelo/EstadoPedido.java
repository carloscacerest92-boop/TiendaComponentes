package com.carloscaceres.tienda.modelo;

/** Ciclo de vida del pedido. Coincide con ck_pedido_estado. */
public enum EstadoPedido {
    PENDIENTE_PAGO,
    PAGADO,
    CANCELADO
}
