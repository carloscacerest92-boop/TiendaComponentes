package com.carloscaceres.tienda.excepcion;

public class StockInsuficienteException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public StockInsuficienteException(String sku, int solicitadas, int disponibles) {
        super("Stock insuficiente de " + sku + ": se solicitan " + solicitadas + " y hay " + disponibles);
    }
}
