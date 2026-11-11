package com.carloscaceres.tienda.bd;

/** Recuento de filas insertadas por la carga inicial, para el log y la verificación. */
public record ResumenCarga(int fabricantes, int sockets, int categorias, int productos,
                           int asignacionesCategoria, int movimientos, int clientes, int direcciones) {

    public int total() {
        // Cada producto tiene además su fila de inventario (1:1).
        return fabricantes + sockets + categorias + productos * 2 + asignacionesCategoria
                + movimientos + clientes + direcciones;
    }
}
