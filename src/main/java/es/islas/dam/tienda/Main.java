package es.islas.dam.tienda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Punto de entrada de la tienda de componentes.
 * <p>
 * Fase 0: solo comprueba que el proyecto compila y que Log4j escribe
 * en consola, en el log activo y en el fichero de evidencia.
 */
public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Arranque de TiendaComponentes · Java {}", Runtime.version());
        log.debug("Directorio de trabajo: {}", System.getProperty("user.dir"));
        log.info("Fin de la ejecución");
    }
}
