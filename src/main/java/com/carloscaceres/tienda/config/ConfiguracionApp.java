package com.carloscaceres.tienda.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Configuración de la aplicación leída de application.properties.
 * <p>
 * Toda la validación se hace aquí, al arrancar: si algo está mal, la aplicación
 * se detiene con un mensaje claro antes de intentar conectar con la base de datos.
 */
public record ConfiguracionApp(MotorBd motor,
                               ModoConexion modoConexion,
                               ModoSimulacion modoSimulacion,
                               DatosConexion datosConexion,
                               ConfiguracionPool pool) {

    public static ConfiguracionApp cargar(Path fichero) {
        if (!Files.exists(fichero)) {
            throw new ConfiguracionException("No existe " + fichero.toAbsolutePath()
                    + ". Copia application.properties.example como application.properties y revisa sus valores.");
        }

        Properties propiedades = new Properties();
        // Se lee como UTF-8 de forma explícita para no depender de la codificación del sistema.
        try (Reader lector = Files.newBufferedReader(fichero, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        } catch (IOException ioe) {
            throw new ConfiguracionException("No se puede leer " + fichero.toAbsolutePath(), ioe);
        }
        return desdePropiedades(propiedades);
    }

    /** Separado de cargar() para poder construir la configuración en las pruebas sin fichero. */
    public static ConfiguracionApp desdePropiedades(Properties propiedades) {
        MotorBd motor = leerEnum(propiedades, "database.engine", MotorBd.class);
        ModoConexion modoConexion = leerEnum(propiedades, "connection.mode", ModoConexion.class);
        ModoSimulacion modoSimulacion = leerEnum(propiedades, "simulation.mode", ModoSimulacion.class);

        // El prefijo de las propiedades de conexión depende del gestor elegido: mysql.* o h2.*
        String prefijo = motor.name().toLowerCase(Locale.ROOT) + ".";
        DatosConexion datos = new DatosConexion(
                leerObligatoria(propiedades, prefijo + "url"),
                leerObligatoria(propiedades, prefijo + "user"),
                propiedades.getProperty(prefijo + "password", "").trim());

        ConfiguracionPool pool = new ConfiguracionPool(
                leerEntero(propiedades, "pool.maximum-size", 5),
                leerEntero(propiedades, "pool.minimum-idle", 1),
                leerEntero(propiedades, "pool.connection-timeout-ms", 3000));

        return new ConfiguracionApp(motor, modoConexion, modoSimulacion, datos, pool);
    }

    private static String leerObligatoria(Properties propiedades, String clave) {
        String valor = propiedades.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new ConfiguracionException("Falta la propiedad obligatoria '" + clave + "'.");
        }
        return valor.trim();
    }

    private static <E extends Enum<E>> E leerEnum(Properties propiedades, String clave, Class<E> tipo) {
        String valor = leerObligatoria(propiedades, clave);
        try {
            return Enum.valueOf(tipo, valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException iae) {
            throw new ConfiguracionException("Valor no válido para '" + clave + "': " + valor
                    + ". Valores admitidos: " + java.util.Arrays.toString(tipo.getEnumConstants()));
        }
    }

    private static int leerEntero(Properties propiedades, String clave, int porDefecto) {
        String valor = propiedades.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        try {
            int numero = Integer.parseInt(valor.trim());
            if (numero <= 0) {
                throw new ConfiguracionException("'" + clave + "' debe ser mayor que 0: " + valor);
            }
            return numero;
        } catch (NumberFormatException nfe) {
            throw new ConfiguracionException("'" + clave + "' debe ser un número entero: " + valor);
        }
    }
}
