package com.carloscaceres.tienda.dto;

import java.math.BigDecimal;

/** Resultado del procedimiento almacenado sp_resumen_ventas_categoria (parámetros OUT). */
public record ResumenVentasCategoriaDto(String categoria, int unidades, BigDecimal ingresos) {
}
