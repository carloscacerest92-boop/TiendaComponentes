package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.Cliente;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ClienteDao {

    private static final Logger log = LogManager.getLogger(ClienteDao.class);

    private static final String COLUMNAS = "id, nombre, apellidos, email, telefono, fecha_alta";
    private static final String SQL_INSERTAR =
            "INSERT INTO cliente (nombre, apellidos, email, telefono) VALUES (?, ?, ?, ?)";

    public int insertar(Connection conexion, Cliente cliente) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarParametros(sentencia, cliente);
            sentencia.executeUpdate();
            int id = JdbcUtils.obtenerClaveGenerada(sentencia);
            cliente.setId(id);
            log.debug("Cliente insertado id={}", id);
            return id;
        }
    }

    public void insertarLote(Connection conexion, List<Cliente> clientes) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            for (Cliente cliente : clientes) {
                asignarParametros(sentencia, cliente);
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, clientes, Cliente::setId);
        }
    }

    public Cliente buscarPorId(Connection conexion, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM cliente WHERE id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    public Cliente buscarPorEmail(Connection conexion, String email) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM cliente WHERE email = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, email);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    public List<Cliente> buscarTodos(Connection conexion) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM cliente ORDER BY id";
        List<Cliente> clientes = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                clientes.add(mapear(resultado));
            }
        }
        return clientes;
    }

    private void asignarParametros(PreparedStatement sentencia, Cliente cliente) throws SQLException {
        sentencia.setString(1, cliente.getNombre());
        sentencia.setString(2, cliente.getApellidos());
        sentencia.setString(3, cliente.getEmail());
        sentencia.setString(4, cliente.getTelefono());
    }

    private Cliente mapear(ResultSet resultado) throws SQLException {
        return new Cliente(
                resultado.getInt("id"),
                resultado.getString("nombre"),
                resultado.getString("apellidos"),
                resultado.getString("email"),
                resultado.getString("telefono"),
                JdbcUtils.getFechaHora(resultado, "fecha_alta"));
    }
}
