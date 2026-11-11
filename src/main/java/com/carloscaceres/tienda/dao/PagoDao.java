package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.EstadoPago;
import com.carloscaceres.tienda.modelo.MetodoPago;
import com.carloscaceres.tienda.modelo.Pago;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PagoDao {

    public int insertar(Connection conexion, Pago pago) throws SQLException {
        String sql = "INSERT INTO pago (pedido_id, importe, metodo, estado, referencia) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, pago.getPedidoId());
            sentencia.setBigDecimal(2, pago.getImporte());
            sentencia.setString(3, pago.getMetodo().name());
            sentencia.setString(4, pago.getEstado().name());
            sentencia.setString(5, pago.getReferencia());
            sentencia.executeUpdate();
            int id = JdbcUtils.obtenerClaveGenerada(sentencia);
            pago.setId(id);
            return id;
        }
    }

    public Pago buscarPorPedido(Connection conexion, int pedidoId) throws SQLException {
        String sql = "SELECT id, pedido_id, importe, metodo, estado, referencia, fecha FROM pago WHERE pedido_id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                return new Pago(
                        resultado.getInt("id"),
                        resultado.getInt("pedido_id"),
                        resultado.getBigDecimal("importe"),
                        MetodoPago.valueOf(resultado.getString("metodo")),
                        EstadoPago.valueOf(resultado.getString("estado")),
                        resultado.getString("referencia"),
                        JdbcUtils.getFechaHora(resultado, "fecha"));
            }
        }
    }

    public int cambiarEstado(Connection conexion, int pagoId, EstadoPago nuevo) throws SQLException {
        String sql = "UPDATE pago SET estado = ? WHERE id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nuevo.name());
            sentencia.setInt(2, pagoId);
            return sentencia.executeUpdate();
        }
    }
}
