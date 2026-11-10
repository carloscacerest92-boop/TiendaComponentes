package com.carloscaceres.tienda.config;

/**
 * Error en el fichero de configuración: falta, no se puede leer o tiene valores no válidos.
 * Es un problema de arranque, no de base de datos ni de negocio.
 */
public class ConfiguracionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfiguracionException(String mensaje) {
        super(mensaje);
    }

    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
