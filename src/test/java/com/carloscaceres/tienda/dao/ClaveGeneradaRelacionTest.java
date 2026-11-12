package com.carloscaceres.tienda.dao;

import com.carloscaceres.tienda.BaseDatosPrueba;
import com.carloscaceres.tienda.modelo.Cliente;
import com.carloscaceres.tienda.modelo.Direccion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaveGeneradaRelacionTest extends BaseDatosPrueba {

    private final ClienteDao clienteDao = new ClienteDao();
    private final DireccionDao direccionDao = new DireccionDao();

    @Test
    @DisplayName("Claves y relaciones: el id generado del cliente enlaza sus direcciones (1:N)")
    void idGeneradoRelacionaRegistros() throws SQLException {
        try (Connection conexion = proveedor.getConnection()) {
            Cliente cliente = new Cliente("Ana", "Prueba Test", "ana.prueba@correo.es", "699000111");
            int clienteId = clienteDao.insertar(conexion, cliente);
            assertTrue(clienteId > 0);

            Direccion casa = new Direccion("Calle Uno 1", "Madrid", "28001", "Madrid", true);
            Direccion trabajo = new Direccion("Calle Dos 2", "Toledo", "45001", "Toledo", false);
            casa.setClienteId(clienteId);
            trabajo.setClienteId(clienteId);
            direccionDao.insertarLote(conexion, List.of(casa, trabajo));
            assertTrue(casa.getId() > 0 && trabajo.getId() > casa.getId(), "Cada dirección recibe su clave");

            List<Direccion> leidas = direccionDao.buscarPorCliente(conexion, clienteId);
            assertEquals(2, leidas.size());
            assertTrue(leidas.stream().allMatch(d -> d.getClienteId() == clienteId));
            assertTrue(leidas.get(0).isPrincipal(), "La principal aparece primero");
        }
    }
}
