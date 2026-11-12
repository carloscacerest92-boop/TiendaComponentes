package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.dto.ResumenVentasCategoriaDto;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/** Llamadas a procedimientos almacenados (solo MySQL) con CallableStatement. */
public class ProcedimientoDao {

    /**
     * {call ...} es la sintaxis de escape de JDBC: el driver la traduce al CALL de cada gestor.
     * Los parámetros OUT se registran ANTES de ejecutar y se leen DESPUÉS.
     */
    public ResumenVentasCategoriaDto resumenVentasCategoria(Connection conexion, String categoria) throws SQLException {
        try (CallableStatement llamada = conexion.prepareCall("{call sp_resumen_ventas_categoria(?, ?, ?)}")) {
            llamada.setString(1, categoria);
            llamada.registerOutParameter(2, Types.INTEGER);
            llamada.registerOutParameter(3, Types.DECIMAL);
            llamada.execute();
            return new ResumenVentasCategoriaDto(categoria, llamada.getInt(2), llamada.getBigDecimal(3));
        }
    }
}
