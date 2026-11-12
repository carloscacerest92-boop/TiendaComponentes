package com.carloscaceres.tienda.bd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lee un script SQL del classpath y lo divide en sentencias.
 * <p>
 * Lector deliberadamente simple: elimina las líneas de comentario (--) y separa por ';'.
 * Por eso los scripts de este proyecto no usan ';' dentro de literales. Los procedimientos
 * almacenados, que sí contienen ';' en su cuerpo, se envían como una única sentencia aparte.
 */
public final class ScriptSql {

    private ScriptSql() {
    }

    public static List<String> leerSentencias(String recurso) {
        String contenido = leerRecurso(recurso);

        String sinComentarios = contenido.lines()
                .filter(linea -> !linea.strip().startsWith("--"))
                .collect(Collectors.joining("\n"));

        return Arrays.stream(sinComentarios.split(";"))
                .map(String::strip)
                .filter(sentencia -> !sentencia.isEmpty())
                .toList();
    }

    /**
     * Lee un script que contiene UNA sola sentencia aunque tenga ';' dentro,
     * como el cuerpo BEGIN ... END de un procedimiento almacenado.
     */
    public static String leerSentenciaUnica(String recurso) {
        return leerRecurso(recurso).lines()
                .filter(linea -> !linea.strip().startsWith("--"))
                .collect(Collectors.joining("\n"))
                .strip();
    }

    private static String leerRecurso(String recurso) {
        try (InputStream entrada = ScriptSql.class.getClassLoader().getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encuentra el recurso " + recurso + " en el classpath.");
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            throw new IllegalStateException("No se puede leer el recurso " + recurso, ioe);
        }
    }
}
