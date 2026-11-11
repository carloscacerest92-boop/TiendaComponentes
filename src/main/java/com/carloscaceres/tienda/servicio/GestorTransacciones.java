package com.carloscaceres.tienda.servicio;

import com.carloscaceres.tienda.conexion.ConnectionProvider;
import com.carloscaceres.tienda.excepcion.FalloSimuladoException;
import com.carloscaceres.tienda.excepcion.NegocioException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Centraliza el patrón de transacción del ejercicio 11 para no repetirlo en cada servicio:
 * <pre>
 *   setAutoCommit(false) -> operación -> commit()
 *                                      -> si falla: rollback() y relanzar
 *   finally: restaurar el autoCommit original
 * </pre>
 * La Connection se abre aquí y se pasa a todos los DAO que participan: por eso todas
 * sus sentencias forman parte de la MISMA transacción.
 */
public class GestorTransacciones {

    private static final Logger log = LogManager.getLogger(GestorTransacciones.class);

    private final ConnectionProvider proveedor;

    public GestorTransacciones(ConnectionProvider proveedor) {
        this.proveedor = proveedor;
    }

    public <T> T ejecutar(String descripcion, OperacionTransaccional<T> operacion)
            throws SQLException, NegocioException {

        try (Connection conexion = proveedor.getConnection()) {
            boolean autoCommitOriginal = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            log.debug("Inicio de transacción: {}", descripcion);

            try {
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                log.debug("Commit: {}", descripcion);
                return resultado;

            } catch (NegocioException ne) {
                // Situación prevista: no es un error técnico, pero tampoco debe quedar nada a medias.
                conexion.rollback();
                log.warn("Operación rechazada ({}): {}", descripcion, ne.getMessage());
                throw ne;

            } catch (FalloSimuladoException fse) {
                log.warn("Fallo controlado; se revierte pedido={}", fse.getPedidoId());
                conexion.rollback();
                log.info("Rollback completado pedido={}", fse.getPedidoId());
                throw fse;

            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                log.error("Error técnico en '{}'; rollback realizado", descripcion, e);
                throw e;

            } finally {
                conexion.setAutoCommit(autoCommitOriginal);
            }
        }
    }
}
