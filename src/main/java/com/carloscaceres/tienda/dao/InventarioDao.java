package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.Inventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class InventarioDao {

    public void insertarLote(Connection conexion, List<Inventario> inventarios) throws SQLException {
        String sql = "INSERT INTO inventario (producto_id, stock, stock_minimo, controla_stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (Inventario inventario : inventarios) {
                sentencia.setInt(1, inventario.getProductoId());
                sentencia.setInt(2, inventario.getStock());
                sentencia.setInt(3, inventario.getStockMinimo());
                sentencia.setBoolean(4, inventario.isControlaStock());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
        }
    }

    public Inventario buscarPorProducto(Connection conexion, int productoId) throws SQLException {
        String sql = "SELECT producto_id, stock, stock_minimo, controla_stock FROM inventario WHERE producto_id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, productoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    /**
     * Suma unidades (reposición o devolución). Devuelve las filas afectadas.
     */
    public int incrementarStock(Connection conexion, int productoId, int unidades) throws SQLException {
        String sql = "UPDATE inventario SET stock = stock + ? WHERE producto_id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, unidades);
            sentencia.setInt(2, productoId);
            return sentencia.executeUpdate();
        }
    }

    /**
     * Resta unidades solo si hay suficientes. La condición "stock >= ?" va en el propio UPDATE:
     * si devuelve 0 filas, no había existencias y no se ha modificado nada.
     */
    public int decrementarStock(Connection conexion, int productoId, int unidades) throws SQLException {
        String sql = "UPDATE inventario SET stock = stock - ? WHERE producto_id = ? AND stock >= ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, unidades);
            sentencia.setInt(2, productoId);
            sentencia.setInt(3, unidades);
            return sentencia.executeUpdate();
        }
    }

    private Inventario mapear(ResultSet resultado) throws SQLException {
        return new Inventario(
                resultado.getInt("producto_id"),
                resultado.getInt("stock"),
                resultado.getInt("stock_minimo"),
                resultado.getBoolean("controla_stock"));
    }
}
