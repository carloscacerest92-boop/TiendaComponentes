package com.carloscaceres.tienda.excepcion;

import com.carloscaceres.tienda.modelo.EstadoPedido;

/** La operación no es válida en el estado actual del pedido (por ejemplo, cancelar dos veces). */
public class EstadoPedidoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public EstadoPedidoException(int pedidoId, EstadoPedido estado, String operacion) {
        super("No se puede " + operacion + " el pedido " + pedidoId + " porque está " + estado);
    }
}
