package com.carloscaceres.tienda.modelo;

/** Tipo técnico del componente. Coincide con los valores admitidos por ck_producto_tipo. */
public enum TipoProducto {
    CPU,
    PLACA_BASE,
    GPU,
    RAM,
    ALMACENAMIENTO,
    FUENTE,
    CAJA,
    REFRIGERACION;

    /** Regla propia de la tienda: solo procesadores y placas base tienen socket. */
    public boolean usaSocket() {
        return this == CPU || this == PLACA_BASE;
    }
}
