package com.carloscaceres.tienda.bd;

import com.carloscaceres.tienda.conexion.ConnectionProvider;
import com.carloscaceres.tienda.dao.CategoriaDao;
import com.carloscaceres.tienda.dao.ClienteDao;
import com.carloscaceres.tienda.dao.DireccionDao;
import com.carloscaceres.tienda.dao.FabricanteDao;
import com.carloscaceres.tienda.dao.InventarioDao;
import com.carloscaceres.tienda.dao.MovimientoStockDao;
import com.carloscaceres.tienda.dao.ProductoDao;
import com.carloscaceres.tienda.dao.SocketDao;
import com.carloscaceres.tienda.modelo.Categoria;
import com.carloscaceres.tienda.modelo.Cliente;
import com.carloscaceres.tienda.modelo.Direccion;
import com.carloscaceres.tienda.modelo.Fabricante;
import com.carloscaceres.tienda.modelo.Inventario;
import com.carloscaceres.tienda.modelo.MovimientoStock;
import com.carloscaceres.tienda.modelo.Producto;
import com.carloscaceres.tienda.modelo.SocketCpu;
import com.carloscaceres.tienda.modelo.TipoMovimiento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga los datos de partida usando lotes (addBatch / executeBatch) dentro de UNA transacción.
 * <p>
 * Son dos ideas distintas:
 * - El batch agrupa muchas sentencias en un solo envío al servidor (menos viajes de red).
 * - La transacción decide si todo se confirma (commit) o se deshace (rollback).
 * Si falla cualquier lote, el rollback deja la base de datos vacía, sin cargas a medias.
 */
public class CargaDatosIniciales {

    private static final Logger log = LogManager.getLogger(CargaDatosIniciales.class);

    private final ConnectionProvider proveedor;
    private final FabricanteDao fabricanteDao = new FabricanteDao();
    private final SocketDao socketDao = new SocketDao();
    private final CategoriaDao categoriaDao = new CategoriaDao();
    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final MovimientoStockDao movimientoDao = new MovimientoStockDao();
    private final ClienteDao clienteDao = new ClienteDao();
    private final DireccionDao direccionDao = new DireccionDao();

    public CargaDatosIniciales(ConnectionProvider proveedor) {
        this.proveedor = proveedor;
    }

    public ResumenCarga cargar() throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            boolean autoCommitOriginal = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            log.debug("Inicio de transacción de carga inicial");
            try {
                ResumenCarga resumen = cargarTodo(conexion);
                conexion.commit();
                log.info("Carga inicial completada (commit): {}", resumen);
                return resumen;
            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                log.error("Fallo en la carga inicial: se revierte la transacción", e);
                throw e;
            } finally {
                conexion.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    private ResumenCarga cargarTodo(Connection conexion) throws SQLException {
        // 1. Tablas sin dependencias. Tras cada lote, cada objeto ya tiene su id generado.
        List<Fabricante> fabricantes = new ArrayList<>();
        DatosIniciales.fabricantes().forEach(f -> fabricantes.add(new Fabricante(f.nombre(), f.pais())));
        fabricanteDao.insertarLote(conexion, fabricantes);

        List<SocketCpu> sockets = new ArrayList<>();
        DatosIniciales.sockets().forEach(nombre -> sockets.add(new SocketCpu(nombre)));
        socketDao.insertarLote(conexion, sockets);

        List<Categoria> categorias = new ArrayList<>();
        DatosIniciales.categorias().forEach(c -> categorias.add(new Categoria(c.nombre(), c.descripcion())));
        categoriaDao.insertarLote(conexion, categorias);

        // Mapas nombre -> id generado, para resolver las referencias de los productos.
        Map<String, Integer> idFabricante = new HashMap<>();
        fabricantes.forEach(f -> idFabricante.put(f.getNombre(), f.getId()));
        Map<String, Integer> idSocket = new HashMap<>();
        sockets.forEach(s -> idSocket.put(s.getNombre(), s.getId()));
        Map<String, Integer> idCategoria = new HashMap<>();
        categorias.forEach(c -> idCategoria.put(c.getNombre(), c.getId()));

        // 2. Productos (dependen de fabricante y socket).
        List<DatosIniciales.ProductoSemilla> semillas = DatosIniciales.productos();
        List<Producto> productos = new ArrayList<>();
        for (DatosIniciales.ProductoSemilla semilla : semillas) {
            Integer socketId = semilla.socket() == null ? null : buscarId(idSocket, semilla.socket(), "socket");
            productos.add(new Producto(semilla.sku(), semilla.nombre(), semilla.tipo(), semilla.precioDecimal(),
                    buscarId(idFabricante, semilla.fabricante(), "fabricante"), socketId));
        }
        productoDao.insertarLote(conexion, productos);

        // 3. Tablas que dependen del id generado de cada producto.
        List<Inventario> inventarios = new ArrayList<>();
        List<MovimientoStock> movimientos = new ArrayList<>();
        Map<Integer, List<Integer>> categoriasPorProducto = new LinkedHashMap<>();
        for (int i = 0; i < semillas.size(); i++) {
            DatosIniciales.ProductoSemilla semilla = semillas.get(i);
            int productoId = productos.get(i).getId();

            inventarios.add(new Inventario(productoId, semilla.stock(), semilla.stockMinimo(), semilla.controlaStock()));
            if (semilla.stock() > 0) {
                // El stock inicial también queda explicado por un movimiento de ENTRADA.
                movimientos.add(new MovimientoStock(productoId, TipoMovimiento.ENTRADA, semilla.stock(),
                        semilla.stock(), null, "Stock inicial"));
            }
            List<Integer> ids = new ArrayList<>();
            for (String nombreCategoria : semilla.categorias()) {
                ids.add(buscarId(idCategoria, nombreCategoria, "categoría"));
            }
            categoriasPorProducto.put(productoId, ids);
        }
        inventarioDao.insertarLote(conexion, inventarios);
        movimientoDao.insertarLote(conexion, movimientos);
        int asignaciones = categoriaDao.asignarLote(conexion, categoriasPorProducto);

        // 4. Clientes y, con su id generado, sus direcciones (1:N).
        List<DatosIniciales.ClienteSemilla> clientesSemilla = DatosIniciales.clientes();
        List<Cliente> clientes = new ArrayList<>();
        clientesSemilla.forEach(c -> clientes.add(new Cliente(c.nombre(), c.apellidos(), c.email(), c.telefono())));
        clienteDao.insertarLote(conexion, clientes);

        List<Direccion> direcciones = new ArrayList<>();
        for (int i = 0; i < clientesSemilla.size(); i++) {
            for (DatosIniciales.DireccionSemilla d : clientesSemilla.get(i).direcciones()) {
                Direccion direccion = new Direccion(d.calle(), d.ciudad(), d.codigoPostal(), d.provincia(), d.principal());
                direccion.setClienteId(clientes.get(i).getId());
                direcciones.add(direccion);
            }
        }
        direccionDao.insertarLote(conexion, direcciones);

        return new ResumenCarga(fabricantes.size(), sockets.size(), categorias.size(), productos.size(),
                asignaciones, movimientos.size(), clientes.size(), direcciones.size());
    }

    private static int buscarId(Map<String, Integer> ids, String nombre, String entidad) {
        Integer id = ids.get(nombre);
        if (id == null) {
            throw new IllegalStateException("Datos iniciales incoherentes: no existe " + entidad + " '" + nombre + "'");
        }
        return id;
    }
}
