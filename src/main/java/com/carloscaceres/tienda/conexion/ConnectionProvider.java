package com.carloscaceres.tienda.conexion;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Origen de las conexiones JDBC.
 * <p>
 * Los DAO y servicios solo conocen esta interfaz: no saben si la Connection
 * viene de DriverManager o de un pool HikariCP. Cambiar de uno a otro es
 * cuestión de configuración, no de código.
 * <p>
 * Quien llama a getConnection() es el propietario de esa Connection y debe cerrarla
 * (con un pool, cerrar significa devolverla al pool, no destruirla).
 */
public interface ConnectionProvider extends AutoCloseable {

    Connection getConnection() throws SQLException;

    /** Libera los recursos del proveedor. Se redefine sin "throws Exception" para usarlo cómodamente. */
    @Override
    void close();
}
