package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.BaseDatosPrueba;
import com.carloscaceres.tienda.config.ModoSimulacion;
import com.carloscaceres.tienda.dto.FiltroCatalogo;
import com.carloscaceres.tienda.dto.Pagina;
import com.carloscaceres.tienda.dto.ProductoCatalogoDto;
import com.carloscaceres.tienda.dto.ProductoVendidoDto;
import com.carloscaceres.tienda.dto.ResumenClienteDto;
import com.carloscaceres.tienda.excepcion.NegocioException;
import com.carloscaceres.tienda.servicio.GestorTransacciones;
import com.carloscaceres.tienda.servicio.LineaSolicitud;
import com.carloscaceres.tienda.servicio.PedidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaDaoTest extends BaseDatosPrueba {

    private final ConsultaDao consultaDao = new ConsultaDao();

    @Test
    @DisplayName("Consulta y DTO: las agregaciones SQL producen los valores esperados")
    void agregacionesEnDto() throws SQLException, NegocioException {
        int clienteId = consultarEntero("SELECT id FROM cliente WHERE email = 'javier.ruiz@correo.es'");
        int direccionId = consultarEntero("SELECT id FROM direccion WHERE cliente_id = ?", clienteId);
        PedidoService servicio = new PedidoService(new GestorTransacciones(proveedor), ModoSimulacion.NORMAL);
        servicio.registrarPedido(clienteId, direccionId, List.of(
                new LineaSolicitud(idProducto("RAM-D5-32-VEN"), 3),
                new LineaSolicitud(idProducto("CASE-H6-FLOW"), 1)));

        try (Connection conexion = proveedor.getConnection()) {
            List<ProductoVendidoDto> ranking = consultaDao.buscarMasVendidos(conexion, 5);
            ProductoVendidoDto primero = ranking.get(0);
            assertEquals("RAM-D5-32-VEN", primero.sku());
            assertEquals(3, primero.unidadesVendidas());
            assertEquals(0, new BigDecimal("389.70").compareTo(primero.ingresos()), "3 x 129.90");

            ResumenClienteDto javier = consultaDao.resumirClientes(conexion).stream()
                    .filter(r -> r.clienteId() == clienteId).findFirst().orElseThrow();
            assertEquals(1, javier.numeroPedidos());
            assertEquals(0, new BigDecimal("499.60").compareTo(javier.totalGastado()), "389.70 + 109.90");
        }
    }

    @Test
    @DisplayName("Paginación: la BD devuelve solo las filas de la página pedida, en orden de precio")
    void catalogoPaginado() throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            Pagina<ProductoCatalogoDto> pagina1 = consultaDao.buscarCatalogo(conexion, FiltroCatalogo.sinFiltros(), 1, 5);
            Pagina<ProductoCatalogoDto> pagina2 = consultaDao.buscarCatalogo(conexion, FiltroCatalogo.sinFiltros(), 2, 5);

            assertEquals(23, pagina1.totalElementos());
            assertEquals(5, pagina1.totalPaginas());
            assertEquals(5, pagina2.contenido().size());
            BigDecimal ultimoPagina1 = pagina1.contenido().get(4).precio();
            assertTrue(pagina2.contenido().stream().allMatch(p -> p.precio().compareTo(ultimoPagina1) >= 0),
                    "La página 2 continúa el orden de la página 1");
            assertTrue(pagina1.contenido().stream().noneMatch(p -> pagina2.contenido().contains(p)),
                    "Las páginas no se solapan");
        }
    }
}
