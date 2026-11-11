package com.carloscaceres.tienda.modelo;

/**
 * Existencias de un producto (relación 1:1: productoId es a la vez PK y FK).
 * controlaStock=false indica un producto "bajo pedido": se vende aunque no haya unidades en almacén.
 */
public class Inventario {

    private final int productoId;
    private int stock;
    private final int stockMinimo;
    private final boolean controlaStock;

    public Inventario(int productoId, int stock, int stockMinimo, boolean controlaStock) {
        this.productoId = productoId;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.controlaStock = controlaStock;
    }

    public int getProductoId() { return productoId; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getStockMinimo() { return stockMinimo; }
    public boolean isControlaStock() { return controlaStock; }

    /** Hay que reponer cuando el stock está en el mínimo o por debajo. */
    public boolean necesitaReposicion() {
        return controlaStock && stock <= stockMinimo;
    }

    @Override
    public String toString() {
        return "Inventario[productoId=" + productoId + ", stock=" + stock + ", minimo=" + stockMinimo
                + ", controlaStock=" + controlaStock + "]";
    }
}
