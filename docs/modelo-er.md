# Modelo entidad-relación · TiendaComponentes

![Diagrama entidad-relación](modelo-er.png)

## Relaciones

| Tipo | Entre | Cómo se resuelve |
|---|---|---|
| 1:N | `fabricante` → `producto` | FK `producto.fabricante_id` |
| 1:N | `socket` → `producto` | FK opcional `producto.socket_id` (solo CPU y placas base) |
| 1:N | `cliente` → `direccion`, `cliente` → `pedido` | FK `cliente_id` |
| 1:N | `pedido` → `linea_pedido` | FK `linea_pedido.pedido_id` |
| 1:N | `producto` → `movimiento_stock` | FK `movimiento_stock.producto_id` |
| **1:1** | `producto` ↔ `inventario` | La PK de `inventario` es a la vez FK a `producto` |
| **1:1** | `pedido` ↔ `pago` | FK `pago.pedido_id` con `UNIQUE` |
| **N:M** | `producto` ↔ `categoria` | Tabla intermedia `producto_categoria` con PK compuesta |
| N:M | `pedido` ↔ `producto` | Tabla intermedia con datos propios: `linea_pedido` |

## Decisiones de diseño

- **`tipo` frente a `categoria`.** El tipo técnico (CPU, placa base, GPU...) es único por producto y lo usan
  las reglas de negocio, por eso es una columna. Las categorías son agrupaciones comerciales
  (Gaming, Ofertas...) y un producto puede estar en varias: de ahí la relación N:M.
- **Compatibilidad por socket (rasgo propio de la tienda).** CPU y placa base guardan su socket.
  Permite la consulta "placas compatibles con una CPU" y la regla que rechaza un pedido que
  combine CPU y placa de sockets distintos. Una restricción `CHECK` garantiza que solo
  CPU y placas tengan socket.
- **Precio histórico.** `linea_pedido.precio_unitario` copia el precio en el momento de la compra:
  si el catálogo cambia después, el pedido conserva su importe.
- **Stock con trazabilidad.** `inventario.stock` es el valor actual y `movimiento_stock` explica cada
  cambio (ENTRADA por reposición, SALIDA por pedido, DEVOLUCION por cancelación). `CHECK (stock >= 0)`
  impide existencias negativas también a nivel de base de datos.
- **Importes con `DECIMAL(10,2)`** (en Java, `BigDecimal`) y fechas con `TIMESTAMP` (`LocalDateTime`).
- **Ciclo de vida del pago.** Se crea al pagar (COMPLETADO); si se cancela un pedido pagado pasa a
  REEMBOLSADO. El estado del pedido impide cancelar dos veces.

## Código del diagrama (Mermaid)

```mermaid
erDiagram
    FABRICANTE ||--o{ PRODUCTO : fabrica
    SOCKET |o--o{ PRODUCTO : "compatible (CPU y placa)"
    PRODUCTO ||--o{ PRODUCTO_CATEGORIA : clasifica
    CATEGORIA ||--o{ PRODUCTO_CATEGORIA : agrupa
    PRODUCTO ||--|| INVENTARIO : "tiene (1:1)"
    PRODUCTO ||--o{ MOVIMIENTO_STOCK : registra
    PEDIDO |o--o{ MOVIMIENTO_STOCK : origina
    CLIENTE ||--o{ DIRECCION : tiene
    CLIENTE ||--o{ PEDIDO : realiza
    DIRECCION ||--o{ PEDIDO : "envío a"
    PEDIDO ||--|{ LINEA_PEDIDO : contiene
    PRODUCTO ||--o{ LINEA_PEDIDO : "comprado en"
    PEDIDO ||--o| PAGO : "se paga (1:1)"

    FABRICANTE {
        int id PK
        varchar nombre UK
        varchar pais
    }
    SOCKET {
        int id PK
        varchar nombre UK "AM5, LGA1700..."
    }
    CATEGORIA {
        int id PK
        varchar nombre UK "Gaming, Ofertas..."
        varchar descripcion
    }
    PRODUCTO {
        int id PK
        varchar sku UK
        varchar nombre
        varchar tipo "CPU, PLACA_BASE, GPU, RAM..."
        decimal precio
        int fabricante_id FK
        int socket_id FK "solo CPU y PLACA_BASE"
        boolean activo
    }
    PRODUCTO_CATEGORIA {
        int producto_id PK,FK
        int categoria_id PK,FK
    }
    INVENTARIO {
        int producto_id PK,FK
        int stock "CHECK >= 0"
        int stock_minimo
        boolean controla_stock
    }
    MOVIMIENTO_STOCK {
        int id PK
        int producto_id FK
        varchar tipo "ENTRADA, SALIDA, DEVOLUCION"
        int cantidad "CHECK > 0"
        int stock_resultante
        int pedido_id FK "NULL en reposiciones"
        varchar motivo
        timestamp fecha
    }
    CLIENTE {
        int id PK
        varchar nombre
        varchar apellidos
        varchar email UK
        varchar telefono
        timestamp fecha_alta
    }
    DIRECCION {
        int id PK
        int cliente_id FK
        varchar calle
        varchar ciudad
        varchar codigo_postal
        varchar provincia
        boolean principal
    }
    PEDIDO {
        int id PK
        int cliente_id FK
        int direccion_id FK
        timestamp fecha
        varchar estado "PENDIENTE_PAGO, PAGADO, CANCELADO"
        decimal total
    }
    LINEA_PEDIDO {
        int id PK
        int pedido_id FK
        int producto_id FK
        int cantidad "CHECK > 0"
        decimal precio_unitario "precio en el momento de compra"
        decimal importe
    }
    PAGO {
        int id PK
        int pedido_id FK,UK
        decimal importe
        varchar metodo "TARJETA, BIZUM, TRANSFERENCIA"
        varchar estado "COMPLETADO, REEMBOLSADO"
        varchar referencia UK
        timestamp fecha
    }
```
