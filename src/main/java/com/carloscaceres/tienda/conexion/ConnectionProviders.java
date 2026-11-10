package com.carloscaceres.tienda.conexion;

import com.carloscaceres.tienda.config.ConfiguracionApp;

/** Único punto donde se decide qué implementación de ConnectionProvider se usa. */
public final class ConnectionProviders {

    private ConnectionProviders() {
    }

    public static ConnectionProvider crear(ConfiguracionApp config) {
        return switch (config.modoConexion()) {
            case DRIVER_MANAGER -> new DriverManagerConnectionProvider(config.datosConexion());
            case POOL -> new HikariConnectionProvider(config.datosConexion(), config.pool());
        };
    }
}
