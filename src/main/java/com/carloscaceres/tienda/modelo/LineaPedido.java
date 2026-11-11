package com.carloscaceres.tienda.modelo;

import java.math.BigDecimal;

/**
 * Producto comprado dentro de un pedido.
 * precioUnitario es una COPIA del precio en el momento de la compra: si después cambia
 * el catálogo, el pedido conserva lo que realmente se cobró.
 */
public class LineaPedido {

    private int id;
    private int pedidoId;
    private final int productoId;
    private final int cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal importe;

    public LineaPedido(int productoId, int cantidad, BigDecimal precioUnitario) {
        this(0, 0, productoId, cantidad, precioUnitario, precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    public LineaPedido(int id, int pedidoId, int productoId, int cantidad, BigDecimal precioUnitario, BigDecimal importe) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.importe = importe;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }
    public int getProductoId() { return productoId; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getImporte() { return importe; }

    @Override
    public String toString() {
        return "LineaPedido[productoId=" + productoId + ", cantidad=" + cantidad + ", precio=" + precioUnitario
                + ", importe=" + importe + "]";
    }
}
