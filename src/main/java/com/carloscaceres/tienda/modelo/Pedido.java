package com.carloscaceres.tienda.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pedido {

    private int id;
    private final int clienteId;
    private final int direccionId;
    private LocalDateTime fecha;
    private EstadoPedido estado;
    private final BigDecimal total;

    public Pedido(int clienteId, int direccionId, BigDecimal total) {
        this(0, clienteId, direccionId, null, EstadoPedido.PENDIENTE_PAGO, total);
    }

    public Pedido(int id, int clienteId, int direccionId, LocalDateTime fecha, EstadoPedido estado, BigDecimal total) {
        this.id = id;
        this.clienteId = clienteId;
        this.direccionId = direccionId;
        this.fecha = fecha;
        this.estado = estado;
        this.total = total;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getClienteId() { return clienteId; }
    public int getDireccionId() { return direccionId; }
    public LocalDateTime getFecha() { return fecha; }
    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }

    @Override
    public String toString() {
        return "Pedido[id=" + id + ", clienteId=" + clienteId + ", estado=" + estado + ", total=" + total + "]";
    }
}
