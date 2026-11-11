package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.LineaPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LineaPedidoDao {

    /** Las líneas de un pedido se insertan en un lote; cada una recibe su id generado. */
    public void insertarLote(Connection conexion, List<LineaPedido> lineas) throws SQLException {
        String sql = """
                INSERT INTO linea_pedido (pedido_id, producto_id, cantidad, precio_unitario, importe)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (LineaPedido linea : lineas) {
                sentencia.setInt(1, linea.getPedidoId());
                sentencia.setInt(2, linea.getProductoId());
                sentencia.setInt(3, linea.getCantidad());
                sentencia.setBigDecimal(4, linea.getPrecioUnitario());
                sentencia.setBigDecimal(5, linea.getImporte());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, lineas, LineaPedido::setId);
        }
    }

    public List<LineaPedido> buscarPorPedido(Connection conexion, int pedidoId) throws SQLException {
        String sql = """
                SELECT id, pedido_id, producto_id, cantidad, precio_unitario, importe
                FROM linea_pedido
                WHERE pedido_id = ?
                ORDER BY id
                """;
        List<LineaPedido> lineas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    lineas.add(new LineaPedido(
                            resultado.getInt("id"),
                            resultado.getInt("pedido_id"),
                            resultado.getInt("producto_id"),
                            resultado.getInt("cantidad"),
                            resultado.getBigDecimal("precio_unitario"),
                            resultado.getBigDecimal("importe")));
                }
            }
        }
        return lineas;
    }

    public int contar(Connection conexion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM linea_pedido");
             ResultSet resultado = sentencia.executeQuery()) {
            resultado.next();
            return resultado.getInt(1);
        }
    }
}
