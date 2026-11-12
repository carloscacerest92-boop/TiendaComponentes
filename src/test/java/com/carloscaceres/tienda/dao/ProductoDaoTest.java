package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.BaseDatosPrueba;
import com.carloscaceres.tienda.modelo.Producto;
import com.carloscaceres.tienda.modelo.TipoProducto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoDaoTest extends BaseDatosPrueba {

    private final ProductoDao productoDao = new ProductoDao();

    @Test
    @DisplayName("Inserción y consulta: el producto se guarda con su clave generada y se recupera igual")
    void insertarYConsultar() throws SQLException {
        int fabricanteId = consultarEntero("SELECT id FROM fabricante WHERE nombre = 'Corsair'");
        Producto nuevo = new Producto("CASE-4000D", "Corsair 4000D Airflow", TipoProducto.CAJA,
                new BigDecimal("94.90"), fabricanteId, null);

        try (Connection conexion = proveedor.getConnection()) {
            int id = productoDao.insertar(conexion, nuevo);
            assertTrue(id > 0, "Debe recibirse una clave generada");
            assertEquals(id, nuevo.getId(), "El id se asigna al objeto");

            Producto leido = productoDao.buscarPorId(conexion, id);
            assertNotNull(leido);
            assertEquals("CASE-4000D", leido.getSku());
            assertEquals(TipoProducto.CAJA, leido.getTipo());
            assertEquals(0, new BigDecimal("94.90").compareTo(leido.getPrecio()));
            assertNull(leido.getSocketId(), "Una caja no tiene socket (NULL)");
        }
    }

    @Test
    @DisplayName("Modificación y eliminación: UPDATE y DELETE cambian el estado persistido")
    void actualizarYEliminar() throws SQLException {
        int id = idProducto("CASE-H6-FLOW");
        try (Connection conexion = proveedor.getConnection()) {
            assertEquals(1, productoDao.actualizarPrecio(conexion, id, new BigDecimal("99.00")));
            assertEquals(0, new BigDecimal("99.00").compareTo(productoDao.buscarPorId(conexion, id).getPrecio()));

            assertEquals(0, productoDao.actualizarPrecio(conexion, 99999, BigDecimal.ONE),
                    "Un id inexistente no afecta a ninguna fila");
        }

        // Un producto nuevo sin inventario ni pedidos se puede borrar.
        int fabricanteId = consultarEntero("SELECT id FROM fabricante WHERE nombre = 'NZXT'");
        try (Connection conexion = proveedor.getConnection()) {
            int nuevo = productoDao.insertar(conexion, new Producto("TMP-1", "Temporal", TipoProducto.CAJA,
                    BigDecimal.TEN, fabricanteId, null));
            assertEquals(1, productoDao.eliminar(conexion, nuevo));
            assertNull(productoDao.buscarPorId(conexion, nuevo));
        }
    }
}
