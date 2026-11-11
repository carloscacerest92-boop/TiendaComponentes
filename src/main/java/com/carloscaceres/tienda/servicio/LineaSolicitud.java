package com.carloscaceres.tienda.servicio;

/** Lo que pide el cliente: qué producto y cuántas unidades. El precio lo pone la tienda. */
public record LineaSolicitud(int productoId, int cantidad) {
}
