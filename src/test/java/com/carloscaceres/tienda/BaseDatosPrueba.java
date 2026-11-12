package com.carloscaceres.tienda;

import com.carloscaceres.tienda.bd.CargaDatosIniciales;
import com.carloscaceres.tienda.bd.InicializadorBaseDatos;
import com.carloscaceres.tienda.conexion.ConnectionProvider;
import com.carloscaceres.tienda.conexion.DriverManagerConnectionProvider;
import com.carloscaceres.tienda.config.DatosConexion;
import com.carloscaceres.tienda.config.MotorBd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Base de las pruebas: antes de CADA prueba crea una base de datos H2 en memoria nueva,
 * con el mismo esquema y los mismos datos iniciales que la aplicación.
 * Así cada prueba es reproducible y no depende de lo que hayan hecho las demás.
 * Se usan operaciones JDBC reales, sin objetos simulados (mocks).
 */
public abstract class BaseDatosPrueba {

    protected ConnectionProvider proveedor;

    @BeforeEach
    void prepararBaseDatos() throws SQLException {
        String url = "jdbc:h2:mem:prueba_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        proveedor = new DriverManagerConnectionProvider(new DatosConexion(url, "sa", ""));
        new InicializadorBaseDatos(proveedor, MotorBd.H2).crearEsquema();
        new CargaDatosIniciales(proveedor).cargar();
    }

    @AfterEach
    void cerrarBaseDatos() throws SQLException {
        try (Connection conexion = proveedor.getConnection();
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SHUTDOWN");
        }
    }

    /** Consulta de apoyo para las aserciones: un único valor entero. */
    protected int consultarEntero(String sql, Object... parametros) throws SQLException {
        try (Connection conexion = proveedor.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
    }

    protected int idProducto(String sku) throws SQLException {
        return consultarEntero("SELECT id FROM producto WHERE sku = ?", sku);
    }

    protected int stock(String sku) throws SQLException {
        return consultarEntero("SELECT i.stock FROM inventario i JOIN producto p ON p.id = i.producto_id WHERE p.sku = ?", sku);
    }
}
