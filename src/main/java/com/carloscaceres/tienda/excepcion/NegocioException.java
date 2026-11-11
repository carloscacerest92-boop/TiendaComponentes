package com.carloscaceres.tienda.excepcion;

/**
 * Situación de negocio PREVISTA que impide completar una operación: falta de stock,
 * cancelación duplicada, productos incompatibles...
 * <p>
 * Se distingue de SQLException (fallo técnico de persistencia) y de ConfiguracionException
 * (problema de infraestructura al arrancar). Es comprobada (checked) para obligar a quien
 * llama a decidir qué hacer con ella.
 */
public class NegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
