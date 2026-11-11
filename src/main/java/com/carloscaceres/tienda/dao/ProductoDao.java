package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.modelo.Producto;
import com.carloscaceres.tienda.modelo.TipoProducto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoDao {

    private static final Logger log = LogManager.getLogger(ProductoDao.class);

    private static final String COLUMNAS = "id, sku, nombre, tipo, precio, fabricante_id, socket_id, activo";
    private static final String SQL_INSERTAR = """
            INSERT INTO producto (sku, nombre, tipo, precio, fabricante_id, socket_id, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    public int insertar(Connection conexion, Producto producto) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarParametros(sentencia, producto);
            sentencia.executeUpdate();
            int id = JdbcUtils.obtenerClaveGenerada(sentencia);
            producto.setId(id);
            log.debug("Producto insertado id={} sku={}", id, producto.getSku());
            return id;
        }
    }

    public void insertarLote(Connection conexion, List<Producto> productos) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            for (Producto producto : productos) {
                asignarParametros(sentencia, producto);
                sentencia.addBatch();
            }
            sentencia.executeBatch();
            JdbcUtils.asignarClavesGeneradas(sentencia, productos, Producto::setId);
        }
    }

    public Producto buscarPorId(Connection conexion, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM producto WHERE id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    public Producto buscarPorSku(Connection conexion, String sku) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM producto WHERE sku = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, sku);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    public List<Producto> buscarTodos(Connection conexion) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM producto ORDER BY id";
        List<Producto> productos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                productos.add(mapear(resultado));
            }
        }
        return productos;
    }

    /** Devuelve las filas afectadas: 0 significa que no existe un producto con ese id. */
    public int actualizarPrecio(Connection conexion, int id, BigDecimal nuevoPrecio) throws SQLException {
        String sql = "UPDATE producto SET precio = ? WHERE id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBigDecimal(1, nuevoPrecio);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate();
        }
    }

    public int eliminar(Connection conexion, int id) throws SQLException {
        String sql = "DELETE FROM producto WHERE id = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            return sentencia.executeUpdate();
        }
    }

    private void asignarParametros(PreparedStatement sentencia, Producto producto) throws SQLException {
        sentencia.setString(1, producto.getSku());
        sentencia.setString(2, producto.getNombre());
        sentencia.setString(3, producto.getTipo().name());
        sentencia.setBigDecimal(4, producto.getPrecio());
        sentencia.setInt(5, producto.getFabricanteId());
        JdbcUtils.setIntegerONull(sentencia, 6, producto.getSocketId());
        sentencia.setBoolean(7, producto.isActivo());
    }

    // Centralizamos el mapeo para no repetir en cada consulta la conversión fila -> Producto.
    private Producto mapear(ResultSet resultado) throws SQLException {
        return new Producto(
                resultado.getInt("id"),
                resultado.getString("sku"),
                resultado.getString("nombre"),
                TipoProducto.valueOf(resultado.getString("tipo")),
                resultado.getBigDecimal("precio"),
                resultado.getInt("fabricante_id"),
                JdbcUtils.getIntegerONull(resultado, "socket_id"),
                resultado.getBoolean("activo"));
    }
}
