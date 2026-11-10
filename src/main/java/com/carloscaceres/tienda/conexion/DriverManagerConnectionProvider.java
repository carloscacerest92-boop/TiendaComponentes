package com.carloscaceres.tienda.conexion;

import com.carloscaceres.tienda.config.DatosConexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Abre una conexión física nueva en cada llamada. Sencillo, pero cada apertura tiene coste. */
public class DriverManagerConnectionProvider implements ConnectionProvider {

    private final DatosConexion datos;

    public DriverManagerConnectionProvider(DatosConexion datos) {
        this.datos = datos;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(datos.url(), datos.usuario(), datos.contrasena());
    }

    @Override
    public void close() {
        // No mantiene recursos propios: cada Connection la cierra quien la pidió.
    }
}
