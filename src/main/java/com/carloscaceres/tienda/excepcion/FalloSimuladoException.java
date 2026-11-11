package com.carloscaceres.tienda.excepcion;

/**
 * Fallo técnico provocado a propósito en modo FALLO_TRANSACCION, después de modificar
 * varias tablas y antes del commit, para demostrar que el rollback no deja cambios parciales.
 * No es una situación de negocio: simula, por ejemplo, una caída a mitad de la operación.
 */
public class FalloSimuladoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int pedidoId;

    public FalloSimuladoException(int pedidoId) {
        super("Fallo simulado antes de confirmar el pedido " + pedidoId);
        this.pedidoId = pedidoId;
    }

    public int getPedidoId() {
        return pedidoId;
    }
}
