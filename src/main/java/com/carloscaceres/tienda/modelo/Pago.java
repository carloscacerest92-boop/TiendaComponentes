package com.carloscaceres.tienda.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pago simulado de un pedido (relación 1:1). No se conecta con ningún servicio bancario. */
public class Pago {

    private int id;
    private final int pedidoId;
    private final BigDecimal importe;
    private final MetodoPago metodo;
    private EstadoPago estado;
    private final String referencia;
    private LocalDateTime fecha;

    public Pago(int pedidoId, BigDecimal importe, MetodoPago metodo, String referencia) {
        this(0, pedidoId, importe, metodo, EstadoPago.COMPLETADO, referencia, null);
    }

    public Pago(int id, int pedidoId, BigDecimal importe, MetodoPago metodo, EstadoPago estado,
                String referencia, LocalDateTime fecha) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.importe = importe;
        this.metodo = metodo;
        this.estado = estado;
        this.referencia = referencia;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPedidoId() { return pedidoId; }
    public BigDecimal getImporte() { return importe; }
    public MetodoPago getMetodo() { return metodo; }
    public EstadoPago getEstado() { return estado; }
    public String getReferencia() { return referencia; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return "Pago[id=" + id + ", pedidoId=" + pedidoId + ", importe=" + importe + ", metodo=" + metodo
                + ", estado=" + estado + ", referencia=" + referencia + "]";
    }
}
