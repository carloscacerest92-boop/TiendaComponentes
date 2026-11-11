package com.carloscaceres.tienda.servicio;

import com.carloscaceres.tienda.dao.InventarioDao;
import com.carloscaceres.tienda.dao.MovimientoStockDao;
import com.carloscaceres.tienda.dao.ProductoDao;
import com.carloscaceres.tienda.excepcion.NegocioException;
import com.carloscaceres.tienda.modelo.Inventario;
import com.carloscaceres.tienda.modelo.MovimientoStock;
import com.carloscaceres.tienda.modelo.Producto;
import com.carloscaceres.tienda.modelo.TipoMovimiento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

public class InventarioService {

    private static final Logger log = LogManager.getLogger(InventarioService.class);

    private final GestorTransacciones transacciones;
    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final MovimientoStockDao movimientoDao = new MovimientoStockDao();

    public InventarioService(GestorTransacciones transacciones) {
        this.transacciones = transacciones;
    }

    /**
     * Reposición: suma unidades y registra el movimiento de ENTRADA.
     * Ambos cambios van en la misma transacción para que el stock nunca cambie sin explicación.
     * Devuelve el stock resultante.
     */
    public int reponer(int productoId, int unidades, String motivo) throws SQLException, NegocioException {
        if (unidades <= 0) {
            throw new NegocioException("La reposición debe ser de al menos 1 unidad (recibido: " + unidades + ")");
        }
        log.info("Reposición producto={} unidades={}", productoId, unidades);

        return transacciones.ejecutar("reposición producto " + productoId, conexion -> {
            Producto producto = productoDao.buscarPorId(conexion, productoId);
            if (producto == null) {
                throw new NegocioException("No existe el producto " + productoId);
            }
            Inventario inventario = inventarioDao.buscarPorProducto(conexion, productoId);
            if (!inventario.isControlaStock()) {
                throw new NegocioException(producto.getSku() + " se sirve bajo pedido y no lleva control de stock");
            }

            inventarioDao.incrementarStock(conexion, productoId, unidades);
            int stockFinal = inventarioDao.buscarPorProducto(conexion, productoId).getStock();
            movimientoDao.insertar(conexion, new MovimientoStock(productoId, TipoMovimiento.ENTRADA, unidades,
                    stockFinal, null, motivo));

            log.info("Reposición completada {}: stock {} -> {}", producto.getSku(), inventario.getStock(), stockFinal);
            return stockFinal;
        });
    }
}
