package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.dto.DetallePedidoDto;
import com.carloscaceres.tienda.dto.LineaDetalleDto;
import com.carloscaceres.tienda.modelo.EstadoPedido;
import com.carloscaceres.tienda.modelo.Pedido;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PedidoDao {

    private static final Logger log = LogManager.getLogger(PedidoDao.class);

    private static final String COLUMNAS = "id, cliente_id, direccion_id, fecha, estado, total";

    public int insertar(Connection conexion, Pedido pedido) throws SQLException {
        String sql = "INSERT INTO pedido (cliente_id, direccion_id, estado, total) VALUES (?, ?, ?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, pedido.getClienteId());
            sentencia.setInt(2, pedido.getDireccionId());
            sentencia.setString(3, pedido.getEstado().name());
            sentencia.setBigDecimal(4, pedido.getTotal());
            sentencia.executeUpdate();
            int id = JdbcUtils.obtenerClaveGenerada(sentencia);
            pedido.setId(id);
            log.debug("Pedido insertado id={}", id);
            return id;
        }
    }

    public Pedido buscarPorId(Connection conexion, int id) throws SQLException {
        return buscar(conexion, "SELECT " + COLUMNAS + " FROM pedido WHERE id = ?", id);
    }

    /**
     * Lee el pedido bloqueando su fila hasta el final de la transacción (SELECT ... FOR UPDATE).
     * Así, dos cancelaciones simultáneas del mismo pedido no pueden leer ambas el estado anterior.
     */
    public Pedido buscarPorIdParaActualizar(Connection conexion, int id) throws SQLException {
        return buscar(conexion, "SELECT " + COLUMNAS + " FROM pedido WHERE id = ? FOR UPDATE", id);
    }

    /**
     * Cambia el estado solo si el pedido sigue en el estado esperado.
     * Devuelve 0 si otro proceso ya lo había cambiado: nunca se pisa un estado inesperado.
     */
    public int cambiarEstado(Connection conexion, int id, EstadoPedido esperado, EstadoPedido nuevo) throws SQLException {
        String sql = "UPDATE pedido SET estado = ? WHERE id = ? AND estado = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nuevo.name());
            sentencia.setInt(2, id);
            sentencia.setString(3, esperado.name());
            return sentencia.executeUpdate();
        }
    }

    public int contar(Connection conexion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT COUNT(*) FROM pedido");
             ResultSet resultado = sentencia.executeQuery()) {
            resultado.next();
            return resultado.getInt(1);
        }
    }

    /**
     * Detalle completo de un pedido. Dos consultas: la cabecera (pedido + cliente + dirección + pago,
     * con LEFT JOIN porque puede no haber pago) y las líneas (linea_pedido + producto).
     */
    public DetallePedidoDto buscarDetalle(Connection conexion, int pedidoId) throws SQLException {
        String sqlCabecera = """
                SELECT p.id, p.fecha, p.estado, p.total,
                       c.nombre, c.apellidos, c.email,
                       d.calle, d.codigo_postal, d.ciudad,
                       pg.estado AS estado_pago
                FROM pedido p
                JOIN cliente c ON c.id = p.cliente_id
                JOIN direccion d ON d.id = p.direccion_id
                LEFT JOIN pago pg ON pg.pedido_id = p.id
                WHERE p.id = ?
                """;
        String sqlLineas = """
                SELECT pr.sku, pr.nombre, l.cantidad, l.precio_unitario, l.importe
                FROM linea_pedido l
                JOIN producto pr ON pr.id = l.producto_id
                WHERE l.pedido_id = ?
                ORDER BY l.id
                """;

        List<LineaDetalleDto> lineas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sqlLineas)) {
            sentencia.setInt(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    lineas.add(new LineaDetalleDto(
                            resultado.getString("sku"),
                            resultado.getString("nombre"),
                            resultado.getInt("cantidad"),
                            resultado.getBigDecimal("precio_unitario"),
                            resultado.getBigDecimal("importe")));
                }
            }
        }

        try (PreparedStatement sentencia = conexion.prepareStatement(sqlCabecera)) {
            sentencia.setInt(1, pedidoId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                String direccion = resultado.getString("calle") + ", " + resultado.getString("codigo_postal")
                        + " " + resultado.getString("ciudad");
                String estadoPago = resultado.getString("estado_pago");
                return new DetallePedidoDto(
                        resultado.getInt("id"),
                        resultado.getString("nombre") + " " + resultado.getString("apellidos"),
                        resultado.getString("email"),
                        direccion,
                        JdbcUtils.getFechaHora(resultado, "fecha"),
                        EstadoPedido.valueOf(resultado.getString("estado")),
                        resultado.getBigDecimal("total"),
                        estadoPago == null ? "SIN PAGO" : estadoPago,
                        lineas);
            }
        }
    }

    private Pedido buscar(Connection conexion, String sql, int id) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    private Pedido mapear(ResultSet resultado) throws SQLException {
        return new Pedido(
                resultado.getInt("id"),
                resultado.getInt("cliente_id"),
                resultado.getInt("direccion_id"),
                JdbcUtils.getFechaHora(resultado, "fecha"),
                EstadoPedido.valueOf(resultado.getString("estado")),
                resultado.getBigDecimal("total"));
    }
}
