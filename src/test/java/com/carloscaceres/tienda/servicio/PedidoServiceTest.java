package com.carloscaceres.tienda.servicio;

import com.carloscaceres.tienda.BaseDatosPrueba;
import com.carloscaceres.tienda.config.ModoSimulacion;
import com.carloscaceres.tienda.excepcion.EstadoPedidoException;
import com.carloscaceres.tienda.excepcion.FalloSimuladoException;
import com.carloscaceres.tienda.excepcion.NegocioException;
import com.carloscaceres.tienda.excepcion.ProductosIncompatiblesException;
import com.carloscaceres.tienda.excepcion.StockInsuficienteException;
import com.carloscaceres.tienda.modelo.MetodoPago;
import com.carloscaceres.tienda.modelo.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PedidoServiceTest extends BaseDatosPrueba {

    private int clienteId;
    private int direccionId;

    @BeforeEach
    void elegirCliente() throws SQLException {
        clienteId = consultarEntero("SELECT id FROM cliente WHERE email = 'lucia.martin@correo.es'");
        direccionId = consultarEntero("SELECT MIN(id) FROM direccion WHERE cliente_id = ?", clienteId);
    }

    private PedidoService servicio(ModoSimulacion modo) {
        return new PedidoService(new GestorTransacciones(proveedor), modo);
    }

    private List<LineaSolicitud> cpuYPlacaAm5() throws SQLException {
        return List.of(new LineaSolicitud(idProducto("CPU-R7-9800X3D"), 1),
                new LineaSolicitud(idProducto("MB-X870-TUF"), 2));
    }

    @Test
    @DisplayName("Commit: registrar un pedido confirma pedido, líneas, stock y movimientos")
    void registrarPedidoConfirmaTodo() throws SQLException, NegocioException {
        Pedido pedido = servicio(ModoSimulacion.NORMAL).registrarPedido(clienteId, direccionId, cpuYPlacaAm5());

        assertEquals(1, consultarEntero("SELECT COUNT(*) FROM pedido WHERE id = ? AND estado = 'PENDIENTE_PAGO'", pedido.getId()));
        assertEquals(2, consultarEntero("SELECT COUNT(*) FROM linea_pedido WHERE pedido_id = ?", pedido.getId()));
        assertEquals(2, consultarEntero("SELECT COUNT(*) FROM movimiento_stock WHERE pedido_id = ? AND tipo = 'SALIDA'", pedido.getId()));
        assertEquals(11, stock("CPU-R7-9800X3D"), "12 - 1");
        assertEquals(5, stock("MB-X870-TUF"), "7 - 2");
        assertEquals(0, new BigDecimal("998.80").compareTo(pedido.getTotal()), "479.00 + 2 x 259.90");
    }

    @Test
    @DisplayName("Rollback: FALLO_TRANSACCION no deja pedido, líneas, movimientos ni stock modificados")
    void falloControladoNoDejaCambiosParciales() throws SQLException {
        int pedidos = consultarEntero("SELECT COUNT(*) FROM pedido");
        int lineas = consultarEntero("SELECT COUNT(*) FROM linea_pedido");
        int movimientos = consultarEntero("SELECT COUNT(*) FROM movimiento_stock");
        List<LineaSolicitud> solicitud = cpuYPlacaAm5();

        FalloSimuladoException fallo = assertThrows(FalloSimuladoException.class,
                () -> servicio(ModoSimulacion.FALLO_TRANSACCION).registrarPedido(clienteId, direccionId, solicitud));

        assertEquals(0, consultarEntero("SELECT COUNT(*) FROM pedido WHERE id = ?", fallo.getPedidoId()));
        assertEquals(pedidos, consultarEntero("SELECT COUNT(*) FROM pedido"));
        assertEquals(lineas, consultarEntero("SELECT COUNT(*) FROM linea_pedido"));
        assertEquals(movimientos, consultarEntero("SELECT COUNT(*) FROM movimiento_stock"));
        assertEquals(12, stock("CPU-R7-9800X3D"));
        assertEquals(7, stock("MB-X870-TUF"));
    }

    @Test
    @DisplayName("Reglas de negocio: sin stock o con sockets distintos el pedido se rechaza sin cambios")
    void reglasDeNegocioRechazanPedido() throws SQLException {
        int pedidos = consultarEntero("SELECT COUNT(*) FROM pedido");
        PedidoService servicio = servicio(ModoSimulacion.NORMAL);
        List<LineaSolicitud> sinStock = List.of(new LineaSolicitud(idProducto("GPU-RTX5090-AST"), 5));
        List<LineaSolicitud> incompatibles = List.of(new LineaSolicitud(idProducto("CPU-I7-14700K"), 1),
                new LineaSolicitud(idProducto("MB-X870-TUF"), 1));

        assertThrows(StockInsuficienteException.class, () -> servicio.registrarPedido(clienteId, direccionId, sinStock));
        assertThrows(ProductosIncompatiblesException.class,
                () -> servicio.registrarPedido(clienteId, direccionId, incompatibles));

        assertEquals(pedidos, consultarEntero("SELECT COUNT(*) FROM pedido"));
        assertEquals(1, stock("GPU-RTX5090-AST"));
    }

    @Test
    @DisplayName("Cancelación: devuelve el stock una sola vez y reembolsa el pago")
    void cancelacionNoDevuelveDosVeces() throws SQLException, NegocioException {
        PedidoService servicio = servicio(ModoSimulacion.NORMAL);
        Pedido pedido = servicio.registrarPedido(clienteId, direccionId, cpuYPlacaAm5());
        servicio.pagarPedido(pedido.getId(), MetodoPago.TARJETA);

        servicio.cancelarPedido(pedido.getId());
        assertEquals(12, stock("CPU-R7-9800X3D"));
        assertEquals(7, stock("MB-X870-TUF"));
        assertEquals(1, consultarEntero("SELECT COUNT(*) FROM pago WHERE pedido_id = ? AND estado = 'REEMBOLSADO'", pedido.getId()));

        assertThrows(EstadoPedidoException.class, () -> servicio.cancelarPedido(pedido.getId()));
        assertEquals(12, stock("CPU-R7-9800X3D"), "La segunda cancelación no vuelve a sumar");
        assertEquals(2, consultarEntero("SELECT COUNT(*) FROM movimiento_stock WHERE pedido_id = ? AND tipo = 'DEVOLUCION'", pedido.getId()));
    }
}
