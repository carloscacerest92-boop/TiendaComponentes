package com.carloscaceres.tienda.config;

/** Parámetros mínimos de HikariCP. Solo se usan con connection.mode=POOL. */
public record ConfiguracionPool(int tamanoMaximo, int minimoInactivas, long timeoutConexionMs) {
}
