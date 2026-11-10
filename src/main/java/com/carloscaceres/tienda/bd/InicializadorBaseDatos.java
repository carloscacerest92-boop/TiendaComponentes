package com.carloscaceres.tienda.bd;

import com.carloscaceres.tienda.conexion.ConnectionProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Reconstruye el esquema de la práctica en cada arranque para partir siempre de un estado conocido.
 * Solo borra y crea las tablas definidas en sql/schema.sql.
 */
public class InicializadorBaseDatos {

    private static final Logger log = LogManager.getLogger(InicializadorBaseDatos.class);
    private static final String SCRIPT_ESQUEMA = "sql/schema.sql";

    private final ConnectionProvider proveedor;

    public InicializadorBaseDatos(ConnectionProvider proveedor) {
        this.proveedor = proveedor;
    }

    public void crearEsquema() throws SQLException {
        List<String> sentencias = ScriptSql.leerSentencias(SCRIPT_ESQUEMA);

        // DDL con Statement: no hay parámetros, así que PreparedStatement no aporta nada.
        // Este método abre la Connection, por eso también es responsable de cerrarla.
        try (Connection conexion = proveedor.getConnection();
             Statement sentencia = conexion.createStatement()) {

            for (String sql : sentencias) {
                log.debug("DDL: {}", primeraLinea(sql));
                sentencia.execute(sql);
            }
        }
        log.info("Esquema reconstruido ({} sentencias DDL)", sentencias.size());
    }

    private static String primeraLinea(String sql) {
        int salto = sql.indexOf('\n');
        return salto < 0 ? sql : sql.substring(0, salto);
    }
}
