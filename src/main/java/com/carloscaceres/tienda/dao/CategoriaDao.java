package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Acceso a categoria y a la tabla intermedia producto_categoria (relación N:M). */
public class CategoriaDao {

    public void insertarLote(Connection conexion, List<Categoria> categorias) throws SQLException {
        String sql = "INSERT INTO categoria (nombre, descripcion) VALUES (?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Categoria categoria : categorias) {
                sentencia.setString(1, categoria.getNombre());
                sentencia.setString(2, categoria.getDescripcion());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, categorias, Categoria::setId);
        }
    }

    /**
     * Inserta en un único lote todas las asignaciones producto -> categorías.
     * Devuelve el número de filas insertadas en la tabla intermedia.
     */
    public int asignarLote(Connection conexion, Map<Integer, List<Integer>> categoriasPorProducto) throws SQLException {
        String sql = "INSERT INTO producto_categoria (producto_id, categoria_id) VALUES (?, ?)";
        int filas = 0;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (Map.Entry<Integer, List<Integer>> entrada : categoriasPorProducto.entrySet()) {
                for (Integer categoriaId : entrada.getValue()) {
                    sentencia.setInt(1, entrada.getKey());
                    sentencia.setInt(2, categoriaId);
                    sentencia.addBatch();
                    filas++;
                }
            }
            sentencia.executeBatch();
        }
        return filas;
    }

    public void asignar(Connection conexion, int productoId, int categoriaId) throws SQLException {
        String sql = "INSERT INTO producto_categoria (producto_id, categoria_id) VALUES (?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, productoId);
            sentencia.setInt(2, categoriaId);
            sentencia.executeUpdate();
        }
    }

    /** Recorre la relación N:M desde el producto: JOIN con la tabla intermedia. */
    public List<Categoria> buscarPorProducto(Connection conexion, int productoId) throws SQLException {
        String sql = """
                SELECT c.id, c.nombre, c.descripcion
                FROM categoria c
                JOIN producto_categoria pc ON pc.categoria_id = c.id
                WHERE pc.producto_id = ?
                ORDER BY c.nombre
                """;
        List<Categoria> categorias = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, productoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    categorias.add(mapear(resultado));
                }
            }
        }
        return categorias;
    }

    public Categoria buscarPorNombre(Connection conexion, String nombre) throws SQLException {
        String sql = "SELECT id, nombre, descripcion FROM categoria WHERE nombre = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombre);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    private Categoria mapear(ResultSet resultado) throws SQLException {
        return new Categoria(resultado.getInt("id"), resultado.getString("nombre"), resultado.getString("descripcion"));
    }
}
