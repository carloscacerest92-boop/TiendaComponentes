package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.Fabricante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla fabricante.
 * Todos los DAO reciben la Connection: no la abren ni la cierran, porque su propietario
 * (el servicio o la carga inicial) decide la transacción en la que participan.
 */
public class FabricanteDao {

    public void insertarLote(Connection conexion, List<Fabricante> fabricantes) throws SQLException {
        String sql = "INSERT INTO fabricante (nombre, pais) VALUES (?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Fabricante fabricante : fabricantes) {
                sentencia.setString(1, fabricante.getNombre());
                sentencia.setString(2, fabricante.getPais());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, fabricantes, Fabricante::setId);
        }
    }

    public List<Fabricante> buscarTodos(Connection conexion) throws SQLException {
        String sql = "SELECT id, nombre, pais FROM fabricante ORDER BY nombre";
        List<Fabricante> fabricantes = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                fabricantes.add(new Fabricante(resultado.getInt("id"), resultado.getString("nombre"),
                        resultado.getString("pais")));
            }
        }
        return fabricantes;
    }
}
