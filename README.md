# TiendaComponentes · Práctica UD2 JDBC

Tienda online de **componentes de PC** que gestiona catálogo, clientes, inventario,
pedidos y pagos simulados sobre una base de datos relacional usando **JDBC directo**.

> Documento en construcción: se completará al final del desarrollo.

## Requisitos

- JDK 25
- Maven (o el integrado en IntelliJ IDEA)
- MySQL Server 8+ (opcional si se usa H2)

## Configuración

1. Copia `application.properties.example` como `application.properties` en la raíz del proyecto.
2. Rellena `mysql.password` con tu contraseña local. Ese fichero no se sube a GitHub.

## Ejecución

```bash
mvn compile exec:java
```

## Pruebas

```bash
mvn test
```
