package com.carloscaceres.tienda.servicio;

import com.carloscaceres.tienda.config.ModoSimulacion;
import com.carloscaceres.tienda.dao.ClienteDao;
import com.carloscaceres.tienda.dao.DireccionDao;
import com.carloscaceres.tienda.dao.InventarioDao;
import com.carloscaceres.tienda.dao.LineaPedidoDao;
import com.carloscaceres.tienda.dao.MovimientoStockDao;
import com.carloscaceres.tienda.dao.PagoDao;
import com.carloscaceres.tienda.dao.PedidoDao;
import com.carloscaceres.tienda.dao.ProductoDao;
import com.carloscaceres.tienda.excepcion.EstadoPedidoException;
import com.carloscaceres.tienda.excepcion.FalloSimuladoException;
import com.carloscaceres.tienda.excepcion.NegocioException;
import com.carloscaceres.tienda.excepcion.ProductosIncompatiblesException;
import com.carloscaceres.tienda.excepcion.StockInsuficienteException;
import com.carloscaceres.tienda.modelo.Cliente;
import com.carloscaceres.tienda.modelo.Direccion;
import com.carloscaceres.tienda.modelo.EstadoPago;
import com.carloscaceres.tienda.modelo.EstadoPedido;
import com.carloscaceres.tienda.modelo.Inventario;
import com.carloscaceres.tienda.modelo.LineaPedido;
import com.carloscaceres.tienda.modelo.MetodoPago;
import com.carloscaceres.tienda.modelo.MovimientoStock;
import com.carloscaceres.tienda.modelo.Pago;
import com.carloscaceres.tienda.modelo.Pedido;
import com.carloscaceres.tienda.modelo.Producto;
import com.carloscaceres.tienda.modelo.TipoMovimiento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Operaciones comerciales sobre pedidos. Cada método público es UNA transacción:
 * coordina varios DAO que comparten la misma Connection y decide commit o rollback.
 */
public class PedidoService {

    private static final Logger log = LogManager.getLogger(PedidoService.class);

    private final GestorTransacciones transacciones;
    private final ModoSimulacion modo;

    private final ClienteDao clienteDao = new ClienteDao();
    private final DireccionDao direccionDao = new DireccionDao();
    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final MovimientoStockDao movimientoDao = new MovimientoStockDao();
    private final PedidoDao pedidoDao = new PedidoDao();
    private final LineaPedidoDao lineaDao = new LineaPedidoDao();
    private final PagoDao pagoDao = new PagoDao();

    public PedidoService(GestorTransacciones transacciones, ModoSimulacion modo) {
        this.transacciones = transacciones;
        this.modo = modo;
    }

    /**
     * Registra un pedido completo de forma atómica:
     * validar cliente, dirección y productos -> comprobar stock y compatibilidad ->
     * crear pedido -> guardar líneas -> descontar stock -> registrar movimientos -> commit.
     * Si algo falla, el rollback deshace todo: no quedan pedidos incompletos ni descuentos parciales.
     */
    public Pedido registrarPedido(int clienteId, int direccionId, List<LineaSolicitud> solicitud)
            throws SQLException, NegocioException {

        validarSolicitud(solicitud);
        log.info("Inicio pedido cliente={} ({} productos distintos)", clienteId, solicitud.size());

        return transacciones.ejecutar("registro de pedido cliente " + clienteId, conexion -> {
            validarClienteYDireccion(conexion, clienteId, direccionId);

            // 1. Cargar productos e inventario y comprobar reglas ANTES de modificar nada.
            Map<Integer, Producto> productos = new HashMap<>();
            Map<Integer, Inventario> inventarios = new HashMap<>();
            for (LineaSolicitud linea : solicitud) {
                Producto producto = productoDao.buscarPorId(conexion, linea.productoId());
                if (producto == null || !producto.isActivo()) {
                    throw new NegocioException("El producto " + linea.productoId() + " no existe o no está disponible");
                }
                Inventario inventario = inventarioDao.buscarPorProducto(conexion, linea.productoId());
                if (inventario.isControlaStock() && inventario.getStock() < linea.cantidad()) {
                    throw new StockInsuficienteException(producto.getSku(), linea.cantidad(), inventario.getStock());
                }
                productos.put(producto.getId(), producto);
                inventarios.put(producto.getId(), inventario);
            }
            comprobarCompatibilidadSocket(productos.values());

            // 2. Líneas con el precio ACTUAL copiado, y total calculado con BigDecimal.
            List<LineaPedido> lineas = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (LineaSolicitud linea : solicitud) {
                LineaPedido lineaPedido = new LineaPedido(linea.productoId(), linea.cantidad(),
                        productos.get(linea.productoId()).getPrecio());
                lineas.add(lineaPedido);
                total = total.add(lineaPedido.getImporte());
            }

            // 3. Pedido -> clave generada -> líneas que la referencian.
            Pedido pedido = new Pedido(clienteId, direccionId, total);
            int pedidoId = pedidoDao.insertar(conexion, pedido);
            lineas.forEach(linea -> linea.setPedidoId(pedidoId));
            lineaDao.insertarLote(conexion, lineas);

            // 4. Descontar stock y dejar constancia en movimiento_stock.
            for (LineaPedido linea : lineas) {
                Inventario inventario = inventarios.get(linea.getProductoId());
                if (!inventario.isControlaStock()) {
                    continue; // Bajo pedido: no hay existencias propias que descontar.
                }
                int filas = inventarioDao.decrementarStock(conexion, linea.getProductoId(), linea.getCantidad());
                if (filas == 0) {
                    // El UPDATE condicionado (stock >= cantidad) protege aunque el stock haya cambiado.
                    Producto producto = productos.get(linea.getProductoId());
                    int disponible = inventarioDao.buscarPorProducto(conexion, producto.getId()).getStock();
                    throw new StockInsuficienteException(producto.getSku(), linea.getCantidad(), disponible);
                }
                int stockFinal = inventarioDao.buscarPorProducto(conexion, linea.getProductoId()).getStock();
                movimientoDao.insertar(conexion, new MovimientoStock(linea.getProductoId(), TipoMovimiento.SALIDA,
                        linea.getCantidad(), stockFinal, pedidoId, "Venta pedido " + pedidoId));
            }

            // 5. Modo FALLO_TRANSACCION: error reproducible DESPUÉS de modificar pedido, líneas,
            //    stock y movimientos, pero ANTES del commit.
            if (modo == ModoSimulacion.FALLO_TRANSACCION) {
                throw new FalloSimuladoException(pedidoId);
            }

            log.info("Pedido registrado id={} total={} €", pedidoId, total);
            return pedido;
        });
    }

    /** Pago simulado: crea el pago COMPLETADO y pasa el pedido a PAGADO, en la misma transacción. */
    public Pago pagarPedido(int pedidoId, MetodoPago metodo) throws SQLException, NegocioException {
        log.info("Pago pedido={} método={}", pedidoId, metodo);

        return transacciones.ejecutar("pago del pedido " + pedidoId, conexion -> {
            Pedido pedido = obtenerPedidoBloqueado(conexion, pedidoId);
            if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
                throw new EstadoPedidoException(pedidoId, pedido.getEstado(), "pagar");
            }

            // Referencia ficticia: no se usa ningún servicio bancario real.
            String referencia = "SIM-" + pedidoId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Pago pago = new Pago(pedidoId, pedido.getTotal(), metodo, referencia);
            pagoDao.insertar(conexion, pago);
            pedidoDao.cambiarEstado(conexion, pedidoId, EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO);

            log.info("Pedido {} pagado: {} € ({}, ref. {})", pedidoId, pago.getImporte(), metodo, referencia);
            return pago;
        });
    }

    /**
     * Cancelación: estado CANCELADO + devolución de unidades + movimientos DEVOLUCION
     * + reembolso del pago si lo había. Un pedido ya cancelado se rechaza, de modo que
     * es imposible devolver dos veces las mismas unidades.
     */
    public void cancelarPedido(int pedidoId) throws SQLException, NegocioException {
        log.info("Cancelación pedido={}", pedidoId);

        transacciones.ejecutar("cancelación del pedido " + pedidoId, conexion -> {
            Pedido pedido = obtenerPedidoBloqueado(conexion, pedidoId);
            if (pedido.getEstado() == EstadoPedido.CANCELADO) {
                throw new EstadoPedidoException(pedidoId, pedido.getEstado(), "cancelar");
            }

            // Doble protección: el UPDATE exige que el estado siga siendo el que acabamos de leer.
            int filas = pedidoDao.cambiarEstado(conexion, pedidoId, pedido.getEstado(), EstadoPedido.CANCELADO);
            if (filas != 1) {
                throw new EstadoPedidoException(pedidoId, pedido.getEstado(), "cancelar");
            }

            for (LineaPedido linea : lineaDao.buscarPorPedido(conexion, pedidoId)) {
                Inventario inventario = inventarioDao.buscarPorProducto(conexion, linea.getProductoId());
                if (!inventario.isControlaStock()) {
                    continue;
                }
                inventarioDao.incrementarStock(conexion, linea.getProductoId(), linea.getCantidad());
                int stockFinal = inventarioDao.buscarPorProducto(conexion, linea.getProductoId()).getStock();
                movimientoDao.insertar(conexion, new MovimientoStock(linea.getProductoId(), TipoMovimiento.DEVOLUCION,
                        linea.getCantidad(), stockFinal, pedidoId, "Cancelación pedido " + pedidoId));
            }

            Pago pago = pagoDao.buscarPorPedido(conexion, pedidoId);
            if (pago != null && pago.getEstado() == EstadoPago.COMPLETADO) {
                pagoDao.cambiarEstado(conexion, pago.getId(), EstadoPago.REEMBOLSADO);
                log.info("Pago {} del pedido {} reembolsado", pago.getReferencia(), pedidoId);
            }

            log.info("Pedido {} cancelado (estado anterior: {})", pedidoId, pedido.getEstado());
            return null;
        });
    }

    private void validarSolicitud(List<LineaSolicitud> solicitud) throws NegocioException {
        if (solicitud == null || solicitud.isEmpty()) {
            throw new NegocioException("Un pedido debe contener al menos un producto");
        }
        Set<Integer> vistos = new HashSet<>();
        for (LineaSolicitud linea : solicitud) {
            if (linea.cantidad() <= 0) {
                throw new NegocioException("La cantidad debe ser positiva (producto " + linea.productoId() + ")");
            }
            if (!vistos.add(linea.productoId())) {
                throw new NegocioException("El producto " + linea.productoId() + " aparece repetido en el pedido");
            }
        }
    }

    private void validarClienteYDireccion(Connection conexion, int clienteId, int direccionId)
            throws SQLException, NegocioException {
        Cliente cliente = clienteDao.buscarPorId(conexion, clienteId);
        if (cliente == null) {
            throw new NegocioException("No existe el cliente " + clienteId);
        }
        Direccion direccion = direccionDao.buscarPorId(conexion, direccionId);
        if (direccion == null || direccion.getClienteId() != clienteId) {
            throw new NegocioException("La dirección " + direccionId + " no pertenece al cliente " + clienteId);
        }
    }

    /** Regla de la tienda: todas las CPU y placas base de un pedido deben tener el mismo socket. */
    private void comprobarCompatibilidadSocket(Iterable<Producto> productos) throws ProductosIncompatiblesException {
        Integer socketComun = null;
        Producto primero = null;
        for (Producto producto : productos) {
            if (!producto.getTipo().usaSocket()) {
                continue;
            }
            if (socketComun == null) {
                socketComun = producto.getSocketId();
                primero = producto;
            } else if (!socketComun.equals(producto.getSocketId())) {
                throw new ProductosIncompatiblesException("Socket incompatible: " + primero.getNombre()
                        + " y " + producto.getNombre() + " no pueden montarse juntos");
            }
        }
    }

    private Pedido obtenerPedidoBloqueado(Connection conexion, int pedidoId) throws SQLException, NegocioException {
        Pedido pedido = pedidoDao.buscarPorIdParaActualizar(conexion, pedidoId);
        if (pedido == null) {
            throw new NegocioException("No existe el pedido " + pedidoId);
        }
        return pedido;
    }
}
