package com.carloscaceres.tienda;

import com.carloscaceres.tienda.bd.CargaDatosIniciales;
import com.carloscaceres.tienda.bd.InicializadorBaseDatos;
import com.carloscaceres.tienda.conexion.ConnectionProvider;
import com.carloscaceres.tienda.conexion.ConnectionProviders;
import com.carloscaceres.tienda.config.ConfiguracionApp;
import com.carloscaceres.tienda.config.ConfiguracionException;
import com.carloscaceres.tienda.simulacion.SimulacionTienda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Punto de entrada de la tienda de componentes.
 * Lee la configuración, prepara las dependencias y lanza la inicialización y la simulación.
 * No contiene SQL: eso pertenece a las capas de base de datos y DAO.
 */
public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);
    private static final Path FICHERO_CONFIGURACION = Path.of("application.properties");

    public static void main(String[] args) {
        log.info("Arranque de TiendaComponentes · Java {}", Runtime.version());

        ConfiguracionApp config;
        try {
            config = ConfiguracionApp.cargar(FICHERO_CONFIGURACION);
        } catch (ConfiguracionException ce) {
            log.error("Configuración no válida: {}", ce.getMessage());
            return;
        }
        log.info("Configuración - Gestor={}, conexiones={}, simulación={}",
                config.motor(), config.modoConexion(), config.modoSimulacion());
        log.debug("Conexión: {}", config.datosConexion());

        // try-with-resources: en modo POOL, al salir se cierra el DataSource de HikariCP.
        try (ConnectionProvider proveedor = ConnectionProviders.crear(config)) {
            new InicializadorBaseDatos(proveedor, config.motor()).crearEsquema();
            new CargaDatosIniciales(proveedor).cargar();
            new SimulacionTienda(proveedor, config.modoSimulacion(), config.motor()).ejecutar();
        } catch (SQLException sqle) {
            log.error("Error de base de datos [SQLState={}, código={}]: {}",
                    sqle.getSQLState(), sqle.getErrorCode(), sqle.getMessage(), sqle);
        }

        log.info("Fin de la ejecución");
    }
}
