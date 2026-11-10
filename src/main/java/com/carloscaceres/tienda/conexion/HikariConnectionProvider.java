package com.carloscaceres.tienda.conexion;

import com.carloscaceres.tienda.config.ConfiguracionPool;
import com.carloscaceres.tienda.config.DatosConexion;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Obtiene las conexiones de un pool HikariCP: se reutilizan conexiones ya abiertas.
 * El DataSource debe cerrarse al terminar la aplicación para liberar todas sus conexiones.
 */
public class HikariConnectionProvider implements ConnectionProvider {

    private final HikariDataSource dataSource;

    public HikariConnectionProvider(DatosConexion datos, ConfiguracionPool pool) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datos.url());
        config.setUsername(datos.usuario());
        config.setPassword(datos.contrasena());
        config.setMaximumPoolSize(pool.tamanoMaximo());
        config.setMinimumIdle(pool.minimoInactivas());
        config.setConnectionTimeout(pool.timeoutConexionMs());
        config.setPoolName("TiendaPool");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
