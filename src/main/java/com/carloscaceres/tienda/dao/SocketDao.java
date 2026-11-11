package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.SocketCpu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SocketDao {

    public void insertarLote(Connection conexion, List<SocketCpu> sockets) throws SQLException {
        String sql = "INSERT INTO socket (nombre) VALUES (?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (SocketCpu socket : sockets) {
                sentencia.setString(1, socket.getNombre());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, sockets, SocketCpu::setId);
        }
    }

    public SocketCpu buscarPorId(Connection conexion, int id) throws SQLException {
        String sql = "SELECT id, nombre FROM socket WHERE id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? new SocketCpu(resultado.getInt("id"), resultado.getString("nombre")) : null;
            }
        }
    }
}
