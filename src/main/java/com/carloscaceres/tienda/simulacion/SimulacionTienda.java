package com.carloscaceres.tienda.simulacion;

import com.carloscaceres.tienda.conexion.ConnectionProvider;
import com.carloscaceres.tienda.config.ModoSimulacion;
import com.carloscaceres.tienda.dao.CategoriaDao;
import com.carloscaceres.tienda.dao.ClienteDao;
import com.carloscaceres.tienda.dao.DireccionDao;
import com.carloscaceres.tienda.dao.InventarioDao;
import com.carloscaceres.tienda.dao.ProductoDao;
import com.carloscaceres.tienda.modelo.Categoria;
import com.carloscaceres.tienda.modelo.Cliente;
import com.carloscaceres.tienda.modelo.Direccion;
import com.carloscaceres.tienda.modelo.Inventario;
import com.carloscaceres.tienda.modelo.Producto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recorrido automático por las operaciones de la tienda.
 * Es la capa de presentación: aquí sí se escribe en consola. Los datos siempre se leen
 * de la base de datos a través de los DAO; nunca se muestran resultados prefabricados.
 */
public class SimulacionTienda {

    private final ConnectionProvider proveedor;
    private final ModoSimulacion modo;
    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final CategoriaDao categoriaDao = new CategoriaDao();
    private final ClienteDao clienteDao = new ClienteDao();
    private final DireccionDao direccionDao = new DireccionDao();

    public SimulacionTienda(ConnectionProvider proveedor, ModoSimulacion modo) {
        this.proveedor = proveedor;
        this.modo = modo;
    }

    public void ejecutar() throws SQLException {
        titulo("SIMULACIÓN DE LA TIENDA · modo " + modo);
        escenarioCatalogo();
        escenarioClientes();
    }

    private void escenarioCatalogo() throws SQLException {
        titulo("1. Catálogo");
        try (Connection conexion = proveedor.getConnection()) {
            List<Producto> productos = productoDao.buscarTodos(conexion);
            System.out.printf("%-4s %-18s %-40s %-15s %10s%n", "ID", "SKU", "NOMBRE", "TIPO", "PRECIO");
            for (Producto producto : productos) {
                System.out.printf("%-4d %-18s %-40s %-15s %9s €%n", producto.getId(), producto.getSku(),
                        producto.getNombre(), producto.getTipo(), producto.getPrecio());
            }
            System.out.println("Total de productos en la base de datos: " + productos.size());

            // Relación N:M y 1:1 recorridas desde un producto concreto.
            Producto ejemplo = productoDao.buscarPorSku(conexion, "CPU-R7-9800X3D");
            List<Categoria> categorias = categoriaDao.buscarPorProducto(conexion, ejemplo.getId());
            Inventario inventario = inventarioDao.buscarPorProducto(conexion, ejemplo.getId());
            System.out.println("\nDetalle de " + ejemplo.getNombre());
            System.out.println("  Categorías (N:M): " + categorias.stream().map(Categoria::getNombre)
                    .collect(Collectors.joining(", ")));
            System.out.println("  Inventario (1:1): stock=" + inventario.getStock()
                    + ", mínimo=" + inventario.getStockMinimo());
        }
    }

    private void escenarioClientes() throws SQLException {
        titulo("2. Clientes");
        try (Connection conexion = proveedor.getConnection()) {
            for (Cliente cliente : clienteDao.buscarTodos(conexion)) {
                System.out.printf("%-3d %-25s %s%n", cliente.getId(), cliente.getNombreCompleto(), cliente.getEmail());
            }

            // Relación 1:N recorrida desde un cliente con varias direcciones.
            Cliente ejemplo = clienteDao.buscarPorEmail(conexion, "lucia.martin@correo.es");
            List<Direccion> direcciones = direccionDao.buscarPorCliente(conexion, ejemplo.getId());
            System.out.println("\nDirecciones de " + ejemplo.getNombreCompleto() + " (1:N):");
            direcciones.forEach(direccion -> System.out.println("  - " + direccion));
        }
    }

    private static void titulo(String texto) {
        System.out.println("\n=== " + texto + " ===");
    }
}
