package com.carloscaceres.tienda.excepcion;

/** Regla propia de la tienda: CPU y placa base de un mismo pedido deben compartir socket. */
public class ProductosIncompatiblesException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public ProductosIncompatiblesException(String mensaje) {
        super(mensaje);
    }
}
