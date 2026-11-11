package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.MovimientoStock;
import com.carloscaceres.tienda.modelo.TipoMovimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MovimientoStockDao {

    private static final String SQL_INSERTAR = """
            INSERT INTO movimiento_stock (producto_id, tipo, cantidad, stock_resultante, pedido_id, motivo)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    public int insertar(Connection conexion, MovimientoStock movimiento) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarParametros(sentencia, movimiento);
            sentencia.executeUpdate();
            int id = JdbcUtils.obtenerClaveGenerada(sentencia);
            movimiento.setId(id);
            return id;
        }
    }

    public void insertarLote(Connection conexion, List<MovimientoStock> movimientos) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            for (MovimientoStock movimiento : movimientos) {
                asignarParametros(sentencia, movimiento);
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, movimientos, MovimientoStock::setId);
        }
    }

    public List<MovimientoStock> buscarPorProducto(Connection conexion, int productoId) throws SQLException {
        String sql = """
                SELECT id, producto_id, tipo, cantidad, stock_resultante, pedido_id, motivo, fecha
                FROM movimiento_stock
                WHERE producto_id = ?
                ORDER BY id
                """;
        List<MovimientoStock> movimientos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, productoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    movimientos.add(mapear(resultado));
                }
            }
        }
        return movimientos;
    }

    public int contarPorPedido(Connection conexion, int pedidoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimiento_stock WHERE pedido_id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
    }

    public int contar(Connection conexion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM movimiento_stock");
             ResultSet resultado = sentencia.executeQuery()) {
            resultado.next();
            return resultado.getInt(1);
        }
    }

    private void asignarParametros(PreparedStatement sentencia, MovimientoStock movimiento) throws SQLException {
        sentencia.setInt(1, movimiento.getProductoId());
        sentencia.setString(2, movimiento.getTipo().name());
        sentencia.setInt(3, movimiento.getCantidad());
        sentencia.setInt(4, movimiento.getStockResultante());
        JdbcUtils.setIntegerONull(sentencia, 5, movimiento.getPedidoId());
        sentencia.setString(6, movimiento.getMotivo());
    }

    private MovimientoStock mapear(ResultSet resultado) throws SQLException {
        return new MovimientoStock(
                resultado.getInt("id"),
                resultado.getInt("producto_id"),
                TipoMovimiento.valueOf(resultado.getString("tipo")),
                resultado.getInt("cantidad"),
                resultado.getInt("stock_resultante"),
                JdbcUtils.getIntegerONull(resultado, "pedido_id"),
                resultado.getString("motivo"),
                JdbcUtils.getFechaHora(resultado, "fecha"));
    }
}
