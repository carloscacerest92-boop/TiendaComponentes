package com.carloscaceres.tienda.servicio;

import com.carloscaceres.tienda.excepcion.NegocioException;

import java.sql.Connection;
import java.sql.SQLException;

/** Trabajo que se ejecuta dentro de una transacción usando una única Connection. */
@FunctionalInterface
public interface OperacionTransaccional<T> {

    T ejecutar(Connection conexion) throws SQLException, NegocioException;
}
