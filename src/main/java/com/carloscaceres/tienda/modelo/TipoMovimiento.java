package com.carloscaceres.tienda.modelo;

/** Motivo de una variación de stock. Coincide con ck_movimiento_tipo. */
public enum TipoMovimiento {
    /** Reposición o stock inicial: suma unidades. */
    ENTRADA,
    /** Venta en un pedido: resta unidades. */
    SALIDA,
    /** Cancelación de un pedido: devuelve unidades. */
    DEVOLUCION
}
