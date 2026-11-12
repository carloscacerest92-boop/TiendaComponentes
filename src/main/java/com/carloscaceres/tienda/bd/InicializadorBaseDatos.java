package com.carloscaceres.tienda.bd;

import com.carloscaceres.tienda.conexion.ConnectionProvider;
import com.carloscaceres.tienda.config.MotorBd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Reconstruye el esquema de la práctica en cada arranque para partir siempre de un estado conocido.
 * Solo borra y crea las tablas definidas en sql/schema.sql y, en MySQL, el procedimiento de la tienda.
 */
public class InicializadorBaseDatos {

    private static final Logger log = LogManager.getLogger(InicializadorBaseDatos.class);
    private static final String SCRIPT_ESQUEMA = "sql/schema.sql";
    private static final String SCRIPT_PROCEDIMIENTO = "sql/mysql/sp_resumen_ventas_categoria.sql";
    public static final String NOMBRE_PROCEDIMIENTO = "sp_resumen_ventas_categoria";

    private final ConnectionProvider proveedor;
    private final MotorBd motor;

    public InicializadorBaseDatos(ConnectionProvider proveedor, MotorBd motor) {
        this.proveedor = proveedor;
        this.motor = motor;
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
            log.info("Esquema reconstruido ({} sentencias DDL)", sentencias.size());

            if (motor == MotorBd.MYSQL) {
                sentencia.execute("DROP PROCEDURE IF EXISTS " + NOMBRE_PROCEDIMIENTO);
                sentencia.execute(ScriptSql.leerSentenciaUnica(SCRIPT_PROCEDIMIENTO));
                log.info("Procedimiento almacenado {} instalado", NOMBRE_PROCEDIMIENTO);
            } else {
                log.info("Procedimiento {} omitido: es específico de MySQL", NOMBRE_PROCEDIMIENTO);
            }
        }
    }

    private static String primeraLinea(String sql) {
        int salto = sql.indexOf('\n');
        return salto < 0 ? sql : sql.substring(0, salto);
    }
}
