package com.carloscaceres.tienda.simulacion;

import com.carloscaceres.tienda.conexion.ConnectionProvider;
import com.carloscaceres.tienda.bd.InicializadorBaseDatos;
import com.carloscaceres.tienda.config.ModoSimulacion;
import com.carloscaceres.tienda.config.MotorBd;
import com.carloscaceres.tienda.dao.CategoriaDao;
import com.carloscaceres.tienda.dao.ConsultaDao;
import com.carloscaceres.tienda.dao.FabricanteDao;
import com.carloscaceres.tienda.dao.ClienteDao;
import com.carloscaceres.tienda.dao.DireccionDao;
import com.carloscaceres.tienda.dao.InventarioDao;
import com.carloscaceres.tienda.dao.LineaPedidoDao;
import com.carloscaceres.tienda.dao.MovimientoStockDao;
import com.carloscaceres.tienda.dao.PedidoDao;
import com.carloscaceres.tienda.dao.ProcedimientoDao;
import com.carloscaceres.tienda.dao.ProductoDao;
import com.carloscaceres.tienda.dto.DetallePedidoDto;
import com.carloscaceres.tienda.dto.FiltroCatalogo;
import com.carloscaceres.tienda.dto.Pagina;
import com.carloscaceres.tienda.dto.PedidoResumenDto;
import com.carloscaceres.tienda.dto.ProductoCatalogoDto;
import com.carloscaceres.tienda.dto.ProductoVendidoDto;
import com.carloscaceres.tienda.dto.ResumenClienteDto;
import com.carloscaceres.tienda.dto.ResumenVentasCategoriaDto;
import com.carloscaceres.tienda.dto.StockBajoDto;
import com.carloscaceres.tienda.dto.LineaDetalleDto;
import com.carloscaceres.tienda.excepcion.FalloSimuladoException;
import com.carloscaceres.tienda.excepcion.NegocioException;
import com.carloscaceres.tienda.modelo.Categoria;
import com.carloscaceres.tienda.modelo.Cliente;
import com.carloscaceres.tienda.modelo.Direccion;
import com.carloscaceres.tienda.modelo.Inventario;
import com.carloscaceres.tienda.modelo.MetodoPago;
import com.carloscaceres.tienda.modelo.MovimientoStock;
import com.carloscaceres.tienda.modelo.Pedido;
import com.carloscaceres.tienda.modelo.Producto;
import com.carloscaceres.tienda.modelo.TipoProducto;
import com.carloscaceres.tienda.servicio.GestorTransacciones;
import com.carloscaceres.tienda.servicio.InventarioService;
import com.carloscaceres.tienda.servicio.LineaSolicitud;
import com.carloscaceres.tienda.servicio.PedidoService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Recorrido automático por las operaciones de la tienda.
 * Es la capa de presentación: aquí sí se escribe en consola. Todo lo que se muestra
 * se lee de la base de datos después de cada operación; nunca son resultados prefabricados.
 */
public class SimulacionTienda {

    private final ConnectionProvider proveedor;
    private final ModoSimulacion modo;
    private final MotorBd motor;
    private final PedidoService pedidoService;
    private final InventarioService inventarioService;

    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();
    private final CategoriaDao categoriaDao = new CategoriaDao();
    private final ClienteDao clienteDao = new ClienteDao();
    private final DireccionDao direccionDao = new DireccionDao();
    private final PedidoDao pedidoDao = new PedidoDao();
    private final LineaPedidoDao lineaDao = new LineaPedidoDao();
    private final MovimientoStockDao movimientoDao = new MovimientoStockDao();
    private final ConsultaDao consultaDao = new ConsultaDao();
    private final ProcedimientoDao procedimientoDao = new ProcedimientoDao();
    private final FabricanteDao fabricanteDao = new FabricanteDao();

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public SimulacionTienda(ConnectionProvider proveedor, ModoSimulacion modo, MotorBd motor) {
        this.proveedor = proveedor;
        this.modo = modo;
        this.motor = motor;
        GestorTransacciones transacciones = new GestorTransacciones(proveedor);
        this.pedidoService = new PedidoService(transacciones, modo);
        this.inventarioService = new InventarioService(transacciones);
    }

    public void ejecutar() throws SQLException {
        titulo("SIMULACIÓN DE LA TIENDA · gestor " + motor + " · modo " + modo);
        escenarioCatalogo();
        if (modo == ModoSimulacion.FALLO_TRANSACCION) {
            escenarioFalloTransaccion();
            return;
        }
        escenarioClientes();
        escenarioReposicion();
        int pedidoLucia = escenarioPedidoYPago();
        escenarioPedidoBajoPedido();
        escenarioPedidosRechazados();
        escenarioCancelacion(pedidoLucia);
        escenarioPrecioHistorico(pedidoLucia);
        escenarioMasActividad();
        escenarioConsultas();
        escenarioIntegridad();
    }

    // ------------------------------------------------------------------ escenarios

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
            Cliente ejemplo = clienteDao.buscarPorEmail(conexion, "lucia.martin@correo.es");
            System.out.println("\nDirecciones de " + ejemplo.getNombreCompleto() + " (1:N):");
            direccionDao.buscarPorCliente(conexion, ejemplo.getId())
                    .forEach(direccion -> System.out.println("  - " + direccion));
        }
    }

    private void escenarioReposicion() throws SQLException {
        titulo("3. Inventario: reposición con movimiento de ENTRADA");
        int productoId = idProducto("COOL-PUREROCK2");
        System.out.println("Stock antes : " + stock(productoId));
        ejecutarOperacion(() -> inventarioService.reponer(productoId, 10, "Reposición proveedor"));
        System.out.println("Stock después: " + stock(productoId));
        mostrarMovimientos(productoId);
    }

    private int escenarioPedidoYPago() throws SQLException {
        titulo("4. Pedido con varios productos + pago simulado");
        int clienteId = idCliente("lucia.martin@correo.es");
        Map<String, Integer> pedido = new LinkedHashMap<>();
        pedido.put("CPU-R7-9800X3D", 1);
        pedido.put("MB-X870-TUF", 1);
        pedido.put("RAM-D5-32-VEN", 2);

        Map<String, Integer> stockAntes = stocks(pedido.keySet());
        Pedido registrado = registrar(clienteId, pedido);
        if (registrado == null) {
            return -1;
        }
        mostrarDetalle(registrado.getId());
        compararStocks(stockAntes, stocks(pedido.keySet()));
        System.out.println("Movimientos de SALIDA del pedido: " + movimientosDePedido(registrado.getId()));

        System.out.println("\n-> Pago con BIZUM");
        ejecutarOperacion(() -> pedidoService.pagarPedido(registrado.getId(), MetodoPago.BIZUM));
        mostrarDetalle(registrado.getId());
        return registrado.getId();
    }

    private void escenarioPedidoBajoPedido() throws SQLException {
        titulo("5. Producto bajo pedido (sin control de stock)");
        int clienteId = idCliente("diego.fernandez@correo.es");
        Map<String, Integer> pedido = new LinkedHashMap<>();
        pedido.put("PSU-FOCUS-GX1000", 1);
        pedido.put("SSD-SN850X-1TB", 1);
        Map<String, Integer> stockAntes = stocks(pedido.keySet());
        Pedido registrado = registrar(clienteId, pedido);
        if (registrado != null) {
            mostrarDetalle(registrado.getId());
            compararStocks(stockAntes, stocks(pedido.keySet()));
        }
    }

    private void escenarioPedidosRechazados() throws SQLException {
        titulo("6. Reglas de negocio: pedidos rechazados");
        int pedidosAntes = contarPedidos();

        System.out.println("-> Javier pide 3 unidades de la RTX 5090 (hay " + stock(idProducto("GPU-RTX5090-AST")) + ")");
        registrar(idCliente("javier.ruiz@correo.es"), Map.of("GPU-RTX5090-AST", 3));

        System.out.println("\n-> Marta pide un Intel i7 (LGA1700) con una placa AM5");
        Map<String, Integer> incompatible = new LinkedHashMap<>();
        incompatible.put("CPU-I7-14700K", 1);
        incompatible.put("MB-X870-TUF", 1);
        registrar(idCliente("marta.sanchez@correo.es"), incompatible);

        System.out.println("\n-> Cantidad no válida");
        registrar(idCliente("marta.sanchez@correo.es"), Map.of("CASE-H6-FLOW", 0));

        System.out.println("\nPedidos en la BD antes: " + pedidosAntes + " · después: " + contarPedidos()
                + " (los rechazos no dejan rastro)");
    }

    private void escenarioCancelacion(int pedidoId) throws SQLException {
        titulo("7. Cancelación del pedido " + pedidoId);
        if (pedidoId < 0) {
            return;
        }
        List<String> skus = List.of("CPU-R7-9800X3D", "MB-X870-TUF", "RAM-D5-32-VEN");
        Map<String, Integer> stockAntes = stocks(skus);
        ejecutarOperacion(() -> {
            pedidoService.cancelarPedido(pedidoId);
            return null;
        });
        mostrarDetalle(pedidoId);
        compararStocks(stockAntes, stocks(skus));
        System.out.println("Movimientos del pedido (SALIDA + DEVOLUCION): " + movimientosDePedido(pedidoId));

        System.out.println("\n-> Segunda cancelación del mismo pedido");
        Map<String, Integer> stockTrasPrimera = stocks(skus);
        ejecutarOperacion(() -> {
            pedidoService.cancelarPedido(pedidoId);
            return null;
        });
        System.out.println("¿Stock sin cambios tras el segundo intento? "
                + (stockTrasPrimera.equals(stocks(skus)) ? "SÍ, no se devuelve dos veces" : "NO (error)"));
    }

    private void escenarioPrecioHistorico(int pedidoId) throws SQLException {
        titulo("8. Modificación de precio y precio histórico");
        if (pedidoId < 0) {
            return;
        }
        int productoId = idProducto("CPU-R7-9800X3D");
        try (Connection conexion = proveedor.getConnection()) {
            BigDecimal anterior = productoDao.buscarPorId(conexion, productoId).getPrecio();
            productoDao.actualizarPrecio(conexion, productoId, new BigDecimal("449.00"));
            System.out.println("Precio en catálogo: " + anterior + " € -> "
                    + productoDao.buscarPorId(conexion, productoId).getPrecio() + " €");
        }
        System.out.println("El pedido " + pedidoId + " conserva el precio con el que se compró:");
        mostrarDetalle(pedidoId);
    }

    /**
     * Modo FALLO_TRANSACCION: se intenta un pedido que falla después de modificar varias tablas.
     * La comprobación se hace con consultas SQL reales antes y después, no con mensajes fijos.
     */
    private void escenarioFalloTransaccion() throws SQLException {
        titulo("FALLO_TRANSACCION: pedido que falla antes del commit");
        int clienteId = idCliente("lucia.martin@correo.es");
        Map<String, Integer> pedido = new LinkedHashMap<>();
        pedido.put("CPU-R5-9600X", 1);
        pedido.put("MB-B650-TOMA", 1);
        pedido.put("SSD-990PRO-2TB", 2);

        EstadoBd antes = capturarEstado(pedido.keySet());
        System.out.println("Estado ANTES   : " + antes);

        int pedidoIntentado = -1;
        try {
            pedidoService.registrarPedido(clienteId, direccionPrincipal(clienteId), solicitud(pedido));
            System.out.println("ERROR: el pedido no debería haberse confirmado");
        } catch (FalloSimuladoException fse) {
            pedidoIntentado = fse.getPedidoId();
            System.out.println("Fallo provocado tras insertar el pedido " + pedidoIntentado
                    + ", sus líneas, descontar stock y registrar movimientos -> ROLLBACK");
        } catch (NegocioException ne) {
            System.out.println("Rechazado por negocio (inesperado aquí): " + ne.getMessage());
        }

        EstadoBd despues = capturarEstado(pedido.keySet());
        System.out.println("Estado DESPUÉS : " + despues);

        boolean pedidoNoExiste;
        try (Connection conexion = proveedor.getConnection()) {
            pedidoNoExiste = pedidoDao.buscarPorId(conexion, pedidoIntentado) == null;
        }
        System.out.println("\nComprobaciones con SQL:");
        System.out.println("  Pedido " + pedidoIntentado + " inexistente en la BD : " + siNo(pedidoNoExiste));
        System.out.println("  Sin líneas parciales            : " + siNo(antes.lineas() == despues.lineas()));
        System.out.println("  Sin movimientos parciales       : " + siNo(antes.movimientos() == despues.movimientos()));
        System.out.println("  Stock idéntico al inicial       : " + siNo(antes.stocks().equals(despues.stocks())));
        System.out.println(antes.equals(despues) && pedidoNoExiste
                ? "RESULTADO: el rollback ha deshecho todas las modificaciones."
                : "RESULTADO: ¡quedan cambios parciales!");
    }

    private void escenarioMasActividad() throws SQLException {
        titulo("9. Más actividad comercial (para las consultas de gestión)");
        Map<String, Integer> elena = new LinkedHashMap<>();
        elena.put("GPU-RTX5070-VEN", 1);
        elena.put("SSD-990PRO-2TB", 1);
        elena.put("RAM-D5-32-VEN", 2);
        Pedido pedidoElena = registrar(idCliente("elena.torres@correo.es"), elena);
        if (pedidoElena != null) {
            ejecutarOperacion(() -> pedidoService.pagarPedido(pedidoElena.getId(), MetodoPago.TARJETA));
        }

        Map<String, Integer> javier = new LinkedHashMap<>();
        javier.put("CPU-R5-9600X", 1);
        javier.put("MB-B650-TOMA", 1);
        javier.put("RAM-D5-32-VEN", 1);
        Pedido pedidoJavier = registrar(idCliente("javier.ruiz@correo.es"), javier);
        if (pedidoJavier != null) {
            ejecutarOperacion(() -> pedidoService.pagarPedido(pedidoJavier.getId(), MetodoPago.TRANSFERENCIA));
        }

        Map<String, Integer> pablo = new LinkedHashMap<>();
        pablo.put("CPU-R5-5600", 1);
        pablo.put("MB-B550-AORUS", 1);
        pablo.put("COOL-PUREROCK2", 1);
        registrar(idCliente("pablo.moreno@correo.es"), pablo);
    }

    private void escenarioConsultas() throws SQLException {
        titulo("10. Consultas de gestión");
        try (Connection conexion = proveedor.getConnection()) {

            System.out.println("-> Catálogo paginado (5 por página, ordenado por precio)");
            for (int numero = 1; numero <= 2; numero++) {
                Pagina<ProductoCatalogoDto> pagina = consultaDao.buscarCatalogo(conexion,
                        FiltroCatalogo.sinFiltros(), numero, 5);
                System.out.printf("  Página %d de %d (%d productos en total)%n", pagina.numeroPagina(),
                        pagina.totalPaginas(), pagina.totalElementos());
                pagina.contenido().forEach(this::imprimirProductoCatalogo);
            }

            System.out.println("\n-> Filtro: categoría Gaming, solo disponibles, entre 100 y 300 €");
            FiltroCatalogo gaming = new FiltroCatalogo("Gaming", null, new BigDecimal("100"),
                    new BigDecimal("300"), true);
            consultaDao.buscarCatalogo(conexion, gaming, 1, 20).contenido().forEach(this::imprimirProductoCatalogo);

            System.out.println("\n-> Filtro: tarjetas gráficas (GPU)");
            FiltroCatalogo gpus = new FiltroCatalogo(null, TipoProducto.GPU, null, null, false);
            consultaDao.buscarCatalogo(conexion, gpus, 1, 20).contenido().forEach(this::imprimirProductoCatalogo);

            System.out.println("\n-> Consulta propia: placas base compatibles con el AMD Ryzen 7 9800X3D");
            consultaDao.buscarPlacasCompatibles(conexion, "CPU-R7-9800X3D").forEach(this::imprimirProductoCatalogo);

            System.out.println("\n-> Resumen de clientes (COUNT / SUM con LEFT JOIN)");
            System.out.printf("  %-25s %8s %10s %12s  %s%n", "CLIENTE", "PEDIDOS", "CANCELADOS", "IMPORTE", "ÚLTIMO PEDIDO");
            for (ResumenClienteDto resumen : consultaDao.resumirClientes(conexion)) {
                System.out.printf("  %-25s %8d %10d %10s €  %s%n", resumen.cliente(), resumen.numeroPedidos(),
                        resumen.pedidosCancelados(), resumen.totalGastado(),
                        resumen.ultimoPedido() == null ? "-" : resumen.ultimoPedido().format(FORMATO_FECHA));
            }

            Cliente lucia = clienteDao.buscarPorEmail(conexion, "lucia.martin@correo.es");
            System.out.println("\n-> Historial de pedidos de " + lucia.getNombreCompleto());
            for (PedidoResumenDto pedido : consultaDao.historialCliente(conexion, lucia.getId())) {
                System.out.printf("  Pedido %d · %s · %-14s · %d productos / %d uds · %s € · envío a %s%n",
                        pedido.pedidoId(), pedido.fecha().format(FORMATO_FECHA), pedido.estado(),
                        pedido.productosDistintos(), pedido.unidades(), pedido.total(), pedido.ciudadEnvio());
            }

            System.out.println("\n-> Productos más vendidos (sin pedidos cancelados)");
            for (ProductoVendidoDto vendido : consultaDao.buscarMasVendidos(conexion, 5)) {
                System.out.printf("  %-18s %-40s %3d uds  %9s €  (%d pedidos)%n", vendido.sku(), vendido.nombre(),
                        vendido.unidadesVendidas(), vendido.ingresos(), vendido.numeroPedidos());
            }

            System.out.println("\n-> Control de existencias: productos que necesitan reposición");
            for (StockBajoDto producto : consultaDao.buscarStockBajo(conexion)) {
                System.out.printf("  %-18s stock %2d / mínimo %2d -> reponer %2d uds (última entrada: %d)%n",
                        producto.sku(), producto.stock(), producto.stockMinimo(), producto.unidadesAReponer(),
                        producto.ultimaEntrada());
            }

            System.out.println("\n-> Procedimiento almacenado " + InicializadorBaseDatos.NOMBRE_PROCEDIMIENTO
                    + " (CallableStatement)");
            if (motor == MotorBd.MYSQL) {
                for (String categoria : List.of("Gaming", "Gama media", "Workstation")) {
                    ResumenVentasCategoriaDto resumen = procedimientoDao.resumenVentasCategoria(conexion, categoria);
                    System.out.printf("  %-12s -> %3d uds vendidas · %9s €%n", resumen.categoria(),
                            resumen.unidades(), resumen.ingresos());
                }
            } else {
                System.out.println("  No disponible con " + motor + ": el procedimiento es específico de MySQL.");
            }
        }
    }

    /**
     * Violaciones de integridad reales provocadas contra la base de datos. Son errores técnicos
     * (SQLException), distintos de las reglas de negocio previstas (NegocioException).
     */
    private void escenarioIntegridad() throws SQLException {
        titulo("11. Integridad de datos y diagnóstico de SQLException");

        System.out.println("-> Cliente con un email que ya existe (UNIQUE)");
        try (Connection conexion = proveedor.getConnection()) {
            clienteDao.insertar(conexion, new Cliente("Otra", "Lucía", "lucia.martin@correo.es", null));
            System.out.println("  ERROR: se ha permitido un email duplicado");
        } catch (SQLException sqle) {
            mostrarSQLException(sqle);
        }

        System.out.println("\n-> Producto con un fabricante inexistente (FOREIGN KEY)");
        try (Connection conexion = proveedor.getConnection()) {
            productoDao.insertar(conexion, new Producto("TEST-FK", "Producto sin fabricante", TipoProducto.CAJA,
                    new BigDecimal("10.00"), 9999, null));
            System.out.println("  ERROR: se ha permitido una referencia inexistente");
        } catch (SQLException sqle) {
            mostrarSQLException(sqle);
        }

        System.out.println("\n-> GPU con socket (CHECK ck_producto_socket: solo CPU y placas base)");
        try (Connection conexion = proveedor.getConnection()) {
            int fabricanteId = fabricanteDao.buscarTodos(conexion).get(0).getId();
            productoDao.insertar(conexion, new Producto("TEST-CHECK", "GPU con socket", TipoProducto.GPU,
                    new BigDecimal("10.00"), fabricanteId, idSocketDe(conexion, "CPU-R7-9800X3D")));
            System.out.println("  ERROR: se ha permitido una GPU con socket");
        } catch (SQLException sqle) {
            mostrarSQLException(sqle);
        }
    }

    private void mostrarSQLException(SQLException sqle) {
        String tipo = sqle instanceof SQLIntegrityConstraintViolationException
                ? "violación de integridad" : "error SQL";
        System.out.println("  Rechazado por la base de datos (" + tipo + ")");
        System.out.println("    SQLState : " + sqle.getSQLState());
        System.out.println("    Código   : " + sqle.getErrorCode());
        System.out.println("    Mensaje  : " + sqle.getMessage().lines().findFirst().orElse(""));
    }

    private Integer idSocketDe(Connection conexion, String sku) throws SQLException {
        return productoDao.buscarPorSku(conexion, sku).getSocketId();
    }

    private void imprimirProductoCatalogo(ProductoCatalogoDto producto) {
        String stock = producto.controlaStock() ? String.valueOf(producto.stock()) : "bajo pedido";
        System.out.printf("    %-18s %-40s %-9s %9s €  stock: %s%n", producto.sku(), producto.nombre(),
                producto.socket() == null ? "" : producto.socket(), producto.precio(), stock);
    }

    // ------------------------------------------------------------------ apoyo

    private record EstadoBd(int pedidos, int lineas, int movimientos, Map<String, Integer> stocks) {
        @Override
        public String toString() {
            return "pedidos=" + pedidos + ", líneas=" + lineas + ", movimientos=" + movimientos + ", stock=" + stocks;
        }
    }

    private EstadoBd capturarEstado(Iterable<String> skus) throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            return new EstadoBd(pedidoDao.contar(conexion), lineaDao.contar(conexion),
                    movimientoDao.contar(conexion), stocks(skus));
        }
    }

    /** Operación de servicio que puede ser rechazada por una regla de negocio. */
    @FunctionalInterface
    private interface Operacion {
        Object ejecutar() throws SQLException, NegocioException;
    }

    private void ejecutarOperacion(Operacion operacion) throws SQLException {
        try {
            Object resultado = operacion.ejecutar();
            System.out.println("  OK" + (resultado == null ? "" : ": " + resultado));
        } catch (NegocioException ne) {
            System.out.println("  RECHAZADO (" + ne.getClass().getSimpleName() + "): " + ne.getMessage());
        }
    }

    private Pedido registrar(int clienteId, Map<String, Integer> productosPorSku) throws SQLException {
        try {
            Pedido pedido = pedidoService.registrarPedido(clienteId, direccionPrincipal(clienteId),
                    solicitud(productosPorSku));
            System.out.println("  OK: pedido " + pedido.getId() + " registrado");
            return pedido;
        } catch (NegocioException ne) {
            System.out.println("  RECHAZADO (" + ne.getClass().getSimpleName() + "): " + ne.getMessage());
            return null;
        }
    }

    private List<LineaSolicitud> solicitud(Map<String, Integer> productosPorSku) throws SQLException {
        List<LineaSolicitud> lineas = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> entrada : productosPorSku.entrySet()) {
            lineas.add(new LineaSolicitud(idProducto(entrada.getKey()), entrada.getValue()));
        }
        return lineas;
    }

    private void mostrarDetalle(int pedidoId) throws SQLException {
        DetallePedidoDto detalle;
        try (Connection conexion = proveedor.getConnection()) {
            detalle = pedidoDao.buscarDetalle(conexion, pedidoId);
        }
        System.out.printf("  Pedido %d · %s <%s>%n", detalle.pedidoId(), detalle.cliente(), detalle.email());
        System.out.printf("  Envío: %s · Estado: %s · Pago: %s%n", detalle.direccionEnvio(), detalle.estado(),
                detalle.estadoPago());
        for (LineaDetalleDto linea : detalle.lineas()) {
            System.out.printf("    %-18s %-40s %2d x %9s € = %9s €%n", linea.sku(), linea.producto(),
                    linea.cantidad(), linea.precioUnitario(), linea.importe());
        }
        System.out.printf("  TOTAL: %s €%n", detalle.total());
    }

    private void mostrarMovimientos(int productoId) throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            System.out.println("Movimientos del producto:");
            for (MovimientoStock movimiento : movimientoDao.buscarPorProducto(conexion, productoId)) {
                System.out.printf("  %-10s %3d uds -> stock %3d · %s%n", movimiento.getTipo(), movimiento.getCantidad(),
                        movimiento.getStockResultante(), movimiento.getMotivo());
            }
        }
    }

    private void compararStocks(Map<String, Integer> antes, Map<String, Integer> despues) {
        System.out.println("  Stock (antes -> después):");
        antes.forEach((sku, valor) -> System.out.printf("    %-18s %3d -> %3d%n", sku, valor, despues.get(sku)));
    }

    private Map<String, Integer> stocks(Iterable<String> skus) throws SQLException {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (String sku : skus) {
            resultado.put(sku, stock(idProducto(sku)));
        }
        return resultado;
    }

    private int stock(int productoId) throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            return inventarioDao.buscarPorProducto(conexion, productoId).getStock();
        }
    }

    private int idProducto(String sku) throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            return productoDao.buscarPorSku(conexion, sku).getId();
        }
    }

    private int idCliente(String email) throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            return clienteDao.buscarPorEmail(conexion, email).getId();
        }
    }

    private int direccionPrincipal(int clienteId) throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            List<Direccion> direcciones = direccionDao.buscarPorCliente(conexion, clienteId);
            return direcciones.get(0).getId(); // ordenadas con la principal primero
        }
    }

    private int movimientosDePedido(int pedidoId) throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            return movimientoDao.contarPorPedido(conexion, pedidoId);
        }
    }

    private int contarPedidos() throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            return pedidoDao.contar(conexion);
        }
    }

    private static String siNo(boolean valor) {
        return valor ? "SÍ" : "NO";
    }

    private static void titulo(String texto) {
        System.out.println("\n=== " + texto + " ===");
    }
}
