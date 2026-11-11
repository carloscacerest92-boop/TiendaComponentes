package com.carloscaceres.tienda.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.ObjIntConsumer;

/** Utilidades JDBC repetidas en varios DAO. */
public final class JdbcUtils {

    private JdbcUtils() {
    }

    /**
     * Tras executeBatch(), recorre las claves generadas y se las asigna a cada objeto en el mismo orden
     * en que se añadieron al lote. Nunca se "adivina" el id ni se consulta MAX(id).
     */
    public static <T> void asignarClavesGeneradas(PreparedStatement sentencia, List<T> objetos,
                                                  ObjIntConsumer<T> asignarId) throws SQLException {
        try (ResultSet claves = sentencia.getGeneratedKeys()) {
            int indice = 0;
            while (claves.next()) {
                if (indice >= objetos.size()) {
                    throw new SQLException("Se han recibido más claves generadas que filas insertadas.");
                }
                asignarId.accept(objetos.get(indice), claves.getInt(1));
                indice++;
            }
            if (indice != objetos.size()) {
                throw new SQLException("Se esperaban " + objetos.size() + " claves generadas y se han recibido " + indice);
            }
        }
    }

    /** Recupera la única clave generada por un INSERT individual. */
    public static int obtenerClaveGenerada(PreparedStatement sentencia) throws SQLException {
        try (ResultSet claves = sentencia.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new SQLException("No se ha recibido la clave generada.");
            }
            return claves.getInt(1);
        }
    }

    /** setInt no admite null: para una columna opcional hay que usar setNull. */
    public static void setIntegerONull(PreparedStatement sentencia, int posicion, Integer valor) throws SQLException {
        if (valor == null) {
            sentencia.setNull(posicion, Types.INTEGER);
        } else {
            sentencia.setInt(posicion, valor);
        }
    }

    /** getInt devuelve 0 cuando la columna es NULL: wasNull() permite distinguir ambos casos. */
    public static Integer getIntegerONull(ResultSet resultado, String columna) throws SQLException {
        int valor = resultado.getInt(columna);
        return resultado.wasNull() ? null : valor;
    }

    public static LocalDateTime getFechaHora(ResultSet resultado, String columna) throws SQLException {
        Timestamp marca = resultado.getTimestamp(columna);
        return marca == null ? null : marca.toLocalDateTime();
    }
}
