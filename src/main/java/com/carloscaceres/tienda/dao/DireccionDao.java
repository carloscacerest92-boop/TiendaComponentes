package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.Direccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DireccionDao {

    private static final String COLUMNAS = "id, cliente_id, calle, ciudad, codigo_postal, provincia, principal";

    public void insertarLote(Connection conexion, List<Direccion> direcciones) throws SQLException {
        String sql = """
                INSERT INTO direccion (cliente_id, calle, ciudad, codigo_postal, provincia, principal)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Direccion direccion : direcciones) {
                sentencia.setInt(1, direccion.getClienteId());
                sentencia.setString(2, direccion.getCalle());
                sentencia.setString(3, direccion.getCiudad());
                sentencia.setString(4, direccion.getCodigoPostal());
                sentencia.setString(5, direccion.getProvincia());
                sentencia.setBoolean(6, direccion.isPrincipal());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, direcciones, Direccion::setId);
        }
    }

    public Direccion buscarPorId(Connection conexion, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM direccion WHERE id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    public List<Direccion> buscarPorCliente(Connection conexion, int clienteId) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM direccion WHERE cliente_id = ? ORDER BY principal DESC, id";
        List<Direccion> direcciones = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, clienteId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    direcciones.add(mapear(resultado));
                }
            }
        }
        return direcciones;
    }

    private Direccion mapear(ResultSet resultado) throws SQLException {
        return new Direccion(
                resultado.getInt("id"),
                resultado.getInt("cliente_id"),
                resultado.getString("calle"),
                resultado.getString("ciudad"),
                resultado.getString("codigo_postal"),
                resultado.getString("provincia"),
                resultado.getBoolean("principal"));
    }
}
