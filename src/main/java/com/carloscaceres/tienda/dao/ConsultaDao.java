package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.dto.FiltroCatalogo;
import com.carloscaceres.tienda.dto.Pagina;
import com.carloscaceres.tienda.dto.PedidoResumenDto;
import com.carloscaceres.tienda.dto.ProductoCatalogoDto;
import com.carloscaceres.tienda.dto.ProductoVendidoDto;
import com.carloscaceres.tienda.dto.ResumenClienteDto;
import com.carloscaceres.tienda.dto.StockBajoDto;
import com.carloscaceres.tienda.modelo.EstadoPedido;
import com.carloscaceres.tienda.modelo.TipoProducto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de gestión. Combinan varias tablas con JOIN, agregan con GROUP BY y
 * devuelven DTO: objetos pensados para el resultado de la consulta, no para una tabla.
 * Los valores calculados (unidades, ingresos, nº de pedidos) se obtienen en SQL,
 * no recorriendo listas en Java.
 */
public class ConsultaDao {

    private static final String SELECT_CATALOGO = """
            SELECT p.id, p.sku, p.nombre, p.tipo, p.precio,
                   f.nombre AS fabricante, s.nombre AS socket,
                   i.stock, i.controla_stock
            FROM producto p
            JOIN fabricante f ON f.id = p.fabricante_id
            JOIN inventario i ON i.producto_id = p.id
            LEFT JOIN socket s ON s.id = p.socket_id
            """;

    /**
     * Catálogo filtrado y paginado. La paginación la hace la base de datos (LIMIT / OFFSET con
     * un ORDER BY fijo): solo viajan las filas de la página pedida, nunca el catálogo entero.
     * Los filtros son opcionales; se añaden al WHERE solo los que tienen valor, siempre como
     * parámetros "?" (nunca concatenando valores del usuario en el SQL).
     */
    public Pagina<ProductoCatalogoDto> buscarCatalogo(Connection conexion, FiltroCatalogo filtro,
                                                      int numeroPagina, int tamanoPagina) throws SQLException {
        if (numeroPagina < 1 || tamanoPagina < 1) {
            throw new IllegalArgumentException("La página y su tamaño deben ser mayores que 0");
        }

        StringBuilder where = new StringBuilder(" WHERE p.activo = TRUE");
        List<Object> parametros = new ArrayList<>();
        if (filtro.categoria() != null) {
            // Ojo: empieza por espacio para no quedar pegado a la condición anterior.
            where.append(" AND EXISTS (SELECT 1 FROM producto_categoria pc"
                    + " JOIN categoria c ON c.id = pc.categoria_id"
                    + " WHERE pc.producto_id = p.id AND c.nombre = ?)");
            parametros.add(filtro.categoria());
        }
        if (filtro.tipo() != null) {
            where.append(" AND p.tipo = ?");
            parametros.add(filtro.tipo().name());
        }
        if (filtro.precioMinimo() != null) {
            where.append(" AND p.precio >= ?");
            parametros.add(filtro.precioMinimo());
        }
        if (filtro.precioMaximo() != null) {
            where.append(" AND p.precio <= ?");
            parametros.add(filtro.precioMaximo());
        }
        if (filtro.soloDisponibles()) {
            where.append(" AND (i.controla_stock = FALSE OR i.stock > 0)");
        }

        // 1. Total de resultados (para saber cuántas páginas hay).
        String sqlTotal = """
                SELECT COUNT(*)
                FROM producto p
                JOIN inventario i ON i.producto_id = p.id
                """ + where;
        int total;
        try (PreparedStatement sentencia = conexion.prepareStatement(sqlTotal)) {
            asignarParametros(sentencia, parametros);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                total = resultado.getInt(1);
            }
        }

        // 2. Solo las filas de la página solicitada.
        String sqlPagina = SELECT_CATALOGO + where + " ORDER BY p.precio, p.id LIMIT ? OFFSET ?";
        List<ProductoCatalogoDto> productos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sqlPagina)) {
            int posicion = asignarParametros(sentencia, parametros);
            sentencia.setInt(posicion, tamanoPagina);
            sentencia.setInt(posicion + 1, (numeroPagina - 1) * tamanoPagina);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    productos.add(mapearCatalogo(resultado));
                }
            }
        }
        return new Pagina<>(productos, numeroPagina, tamanoPagina, total);
    }

    /**
     * Consulta propia de la tienda: placas base compatibles con una CPU (mismo socket).
     * Self-join de producto: "cpu" y "placa" son dos alias de la misma tabla.
     */
    public List<ProductoCatalogoDto> buscarPlacasCompatibles(Connection conexion, String skuCpu) throws SQLException {
        String sql = SELECT_CATALOGO + """
                JOIN producto cpu ON cpu.socket_id = p.socket_id
                WHERE cpu.sku = ? AND cpu.tipo = 'CPU' AND p.tipo = 'PLACA_BASE' AND p.activo = TRUE
                ORDER BY p.precio
                """;
        List<ProductoCatalogoDto> placas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, skuCpu);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    placas.add(mapearCatalogo(resultado));
                }
            }
        }
        return placas;
    }

    /**
     * Resumen de actividad de cada cliente. LEFT JOIN para incluir también a los clientes
     * sin pedidos (con 0). El gasto no cuenta los pedidos cancelados.
     */
    public List<ResumenClienteDto> resumirClientes(Connection conexion) throws SQLException {
        String sql = """
                SELECT c.id, c.nombre, c.apellidos, c.email,
                       COUNT(p.id) AS num_pedidos,
                       SUM(CASE WHEN p.estado = 'CANCELADO' THEN 1 ELSE 0 END) AS cancelados,
                       COALESCE(SUM(CASE WHEN p.estado <> 'CANCELADO' THEN p.total END), 0) AS gastado,
                       MAX(p.fecha) AS ultimo
                FROM cliente c
                LEFT JOIN pedido p ON p.cliente_id = c.id
                GROUP BY c.id, c.nombre, c.apellidos, c.email
                ORDER BY gastado DESC, c.id
                """;
        List<ResumenClienteDto> resumenes = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                resumenes.add(new ResumenClienteDto(
                        resultado.getInt("id"),
                        resultado.getString("nombre") + " " + resultado.getString("apellidos"),
                        resultado.getString("email"),
                        resultado.getInt("num_pedidos"),
                        resultado.getInt("cancelados"),
                        resultado.getBigDecimal("gastado"),
                        JdbcUtils.getFechaHora(resultado, "ultimo")));
            }
        }
        return resumenes;
    }

    /** Historial de pedidos de un cliente con unidades y nº de productos calculados por pedido. */
    public List<PedidoResumenDto> historialCliente(Connection conexion, int clienteId) throws SQLException {
        String sql = """
                SELECT p.id, p.fecha, p.estado, p.total, d.ciudad,
                       COUNT(l.id) AS productos, SUM(l.cantidad) AS unidades
                FROM pedido p
                JOIN direccion d ON d.id = p.direccion_id
                JOIN linea_pedido l ON l.pedido_id = p.id
                WHERE p.cliente_id = ?
                GROUP BY p.id, p.fecha, p.estado, p.total, d.ciudad
                ORDER BY p.fecha DESC, p.id DESC
                """;
        List<PedidoResumenDto> pedidos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, clienteId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    pedidos.add(new PedidoResumenDto(
                            resultado.getInt("id"),
                            JdbcUtils.getFechaHora(resultado, "fecha"),
                            EstadoPedido.valueOf(resultado.getString("estado")),
                            resultado.getInt("productos"),
                            resultado.getInt("unidades"),
                            resultado.getBigDecimal("total"),
                            resultado.getString("ciudad")));
                }
            }
        }
        return pedidos;
    }

    /** Productos más vendidos (pedidos no cancelados), agregando las líneas de pedido. */
    public List<ProductoVendidoDto> buscarMasVendidos(Connection conexion, int limite) throws SQLException {
        String sql = """
                SELECT pr.sku, pr.nombre,
                       SUM(l.cantidad) AS unidades,
                       SUM(l.importe) AS ingresos,
                       COUNT(DISTINCT p.id) AS pedidos
                FROM linea_pedido l
                JOIN pedido p ON p.id = l.pedido_id
                JOIN producto pr ON pr.id = l.producto_id
                WHERE p.estado <> 'CANCELADO'
                GROUP BY pr.id, pr.sku, pr.nombre
                ORDER BY unidades DESC, ingresos DESC
                LIMIT ?
                """;
        List<ProductoVendidoDto> ranking = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, limite);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    ranking.add(new ProductoVendidoDto(
                            resultado.getString("sku"),
                            resultado.getString("nombre"),
                            resultado.getInt("unidades"),
                            resultado.getBigDecimal("ingresos"),
                            resultado.getInt("pedidos")));
                }
            }
        }
        return ranking;
    }

    /**
     * Control de existencias: productos en el mínimo o por debajo, con las unidades a reponer
     * y la cantidad de la última entrada registrada (subconsulta sobre movimiento_stock).
     */
    public List<StockBajoDto> buscarStockBajo(Connection conexion) throws SQLException {
        String sql = """
                SELECT pr.sku, pr.nombre, i.stock, i.stock_minimo,
                       (i.stock_minimo * 2 - i.stock) AS a_reponer,
                       COALESCE((SELECT m.cantidad FROM movimiento_stock m
                                 WHERE m.producto_id = pr.id AND m.tipo = 'ENTRADA'
                                 ORDER BY m.id DESC LIMIT 1), 0) AS ultima_entrada
                FROM inventario i
                JOIN producto pr ON pr.id = i.producto_id
                WHERE i.controla_stock = TRUE AND i.stock <= i.stock_minimo
                ORDER BY (i.stock - i.stock_minimo), pr.sku
                """;
        List<StockBajoDto> productos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                productos.add(new StockBajoDto(
                        resultado.getString("sku"),
                        resultado.getString("nombre"),
                        resultado.getInt("stock"),
                        resultado.getInt("stock_minimo"),
                        resultado.getInt("a_reponer"),
                        resultado.getInt("ultima_entrada")));
            }
        }
        return productos;
    }

    private int asignarParametros(PreparedStatement sentencia, List<Object> parametros) throws SQLException {
        int posicion = 1;
        for (Object parametro : parametros) {
            sentencia.setObject(posicion++, parametro);
        }
        return posicion;
    }

    private ProductoCatalogoDto mapearCatalogo(ResultSet resultado) throws SQLException {
        return new ProductoCatalogoDto(
                resultado.getInt("id"),
                resultado.getString("sku"),
                resultado.getString("nombre"),
                TipoProducto.valueOf(resultado.getString("tipo")),
                resultado.getBigDecimal("precio"),
                resultado.getString("fabricante"),
                resultado.getString("socket"),
                resultado.getInt("stock"),
                resultado.getBoolean("controla_stock"));
    }
}
