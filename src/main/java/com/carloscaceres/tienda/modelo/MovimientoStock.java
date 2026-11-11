package com.carloscaceres.tienda.modelo;

import java.time.LocalDateTime;

/** Registro que explica una variación de stock. pedidoId es null en las reposiciones. */
public class MovimientoStock {

    private int id;
    private final int productoId;
    private final TipoMovimiento tipo;
    private final int cantidad;
    private final int stockResultante;
    private final Integer pedidoId;
    private final String motivo;
    private LocalDateTime fecha;

    public MovimientoStock(int productoId, TipoMovimiento tipo, int cantidad, int stockResultante,
                           Integer pedidoId, String motivo) {
        this(0, productoId, tipo, cantidad, stockResultante, pedidoId, motivo, null);
    }

    public MovimientoStock(int id, int productoId, TipoMovimiento tipo, int cantidad, int stockResultante,
                           Integer pedidoId, String motivo, LocalDateTime fecha) {
        this.id = id;
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockResultante = stockResultante;
        this.pedidoId = pedidoId;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductoId() { return productoId; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getStockResultante() { return stockResultante; }
    public Integer getPedidoId() { return pedidoId; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return "MovimientoStock[id=" + id + ", productoId=" + productoId + ", tipo=" + tipo
                + ", cantidad=" + cantidad + ", resultante=" + stockResultante + ", pedidoId=" + pedidoId + "]";
    }
}
