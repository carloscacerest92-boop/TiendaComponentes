package com.carloscaceres.tienda.modelo;

import java.math.BigDecimal;

/**
 * Producto del catálogo.
 * El precio usa BigDecimal: con double, 0.1 + 0.2 no da exactamente 0.3 y los importes acumularían errores.
 * socketId es Integer (no int) porque puede ser NULL: solo CPU y placas base tienen socket.
 */
public class Producto {

    private int id;
    private String sku;
    private String nombre;
    private TipoProducto tipo;
    private BigDecimal precio;
    private int fabricanteId;
    private Integer socketId;
    private boolean activo;

    public Producto(String sku, String nombre, TipoProducto tipo, BigDecimal precio,
                    int fabricanteId, Integer socketId) {
        this(0, sku, nombre, tipo, precio, fabricanteId, socketId, true);
    }

    public Producto(int id, String sku, String nombre, TipoProducto tipo, BigDecimal precio,
                    int fabricanteId, Integer socketId, boolean activo) {
        this.id = id;
        this.sku = sku;
        this.nombre = nombre;
        this.tipo = tipo;
        this.precio = precio;
        this.fabricanteId = fabricanteId;
        this.socketId = socketId;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public TipoProducto getTipo() { return tipo; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public int getFabricanteId() { return fabricanteId; }
    public Integer getSocketId() { return socketId; }
    public boolean isActivo() { return activo; }

    @Override
    public String toString() {
        return "Producto[id=" + id + ", sku=" + sku + ", nombre=" + nombre + ", tipo=" + tipo
                + ", precio=" + precio + ", socketId=" + socketId + "]";
    }
}
