-- =====================================================================
-- TiendaComponentes · Esquema de la base de datos (unidad2)
-- Script común para MySQL 8 y H2 (MODE=MySQL).
-- Cada ejecución reconstruye SOLO las tablas de esta práctica.
-- Formato: una sentencia por bloque terminado en ';' y sin ';' dentro
-- de literales (el lector de scripts divide por ese carácter).
-- =====================================================================

-- Borrado en orden inverso de dependencias: primero las tablas que
-- tienen claves foráneas, después las tablas a las que apuntan.
DROP TABLE IF EXISTS pago;
DROP TABLE IF EXISTS linea_pedido;
DROP TABLE IF EXISTS movimiento_stock;
DROP TABLE IF EXISTS pedido;
DROP TABLE IF EXISTS direccion;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS inventario;
DROP TABLE IF EXISTS producto_categoria;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS socket;
DROP TABLE IF EXISTS fabricante;

-- ---------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------
CREATE TABLE fabricante (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(80) NOT NULL,
    pais    VARCHAR(60),
    CONSTRAINT uk_fabricante_nombre UNIQUE (nombre)
);

CREATE TABLE socket (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(30) NOT NULL,
    CONSTRAINT uk_socket_nombre UNIQUE (nombre)
);

CREATE TABLE categoria (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(60) NOT NULL,
    descripcion  VARCHAR(255),
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
);

CREATE TABLE producto (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    sku            VARCHAR(30)   NOT NULL,
    nombre         VARCHAR(150)  NOT NULL,
    tipo           VARCHAR(20)   NOT NULL,
    precio         DECIMAL(10,2) NOT NULL,
    fabricante_id  INT           NOT NULL,
    socket_id      INT,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_producto_sku UNIQUE (sku),
    CONSTRAINT ck_producto_tipo CHECK (tipo IN ('CPU', 'PLACA_BASE', 'GPU', 'RAM',
                                                'ALMACENAMIENTO', 'FUENTE', 'CAJA', 'REFRIGERACION')),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    -- Regla propia de la tienda: solo CPU y placas base tienen socket, y lo tienen obligatoriamente.
    CONSTRAINT ck_producto_socket CHECK (
        (tipo IN ('CPU', 'PLACA_BASE') AND socket_id IS NOT NULL)
        OR (tipo NOT IN ('CPU', 'PLACA_BASE') AND socket_id IS NULL)),
    CONSTRAINT fk_producto_fabricante FOREIGN KEY (fabricante_id) REFERENCES fabricante (id),
    CONSTRAINT fk_producto_socket FOREIGN KEY (socket_id) REFERENCES socket (id)
);

-- Relación N:M producto <-> categoria. La PK compuesta impide repetir el mismo par.
CREATE TABLE producto_categoria (
    producto_id   INT NOT NULL,
    categoria_id  INT NOT NULL,
    PRIMARY KEY (producto_id, categoria_id),
    CONSTRAINT fk_prodcat_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_prodcat_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);

-- ---------------------------------------------------------------------
-- Inventario
-- ---------------------------------------------------------------------
-- Relación 1:1 con producto: la PK es también FK, así que no puede haber dos filas por producto.
CREATE TABLE inventario (
    producto_id     INT     PRIMARY KEY,
    stock           INT     NOT NULL DEFAULT 0,
    stock_minimo    INT     NOT NULL DEFAULT 0,
    controla_stock  BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_inventario_stock CHECK (stock >= 0),
    CONSTRAINT ck_inventario_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT fk_inventario_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

-- ---------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(60)  NOT NULL,
    apellidos   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    telefono    VARCHAR(20),
    fecha_alta  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cliente_email UNIQUE (email)
);

CREATE TABLE direccion (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id     INT          NOT NULL,
    calle          VARCHAR(150) NOT NULL,
    ciudad         VARCHAR(80)  NOT NULL,
    codigo_postal  CHAR(5)      NOT NULL,
    provincia      VARCHAR(60)  NOT NULL,
    principal      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_direccion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

-- ---------------------------------------------------------------------
-- Pedidos y pagos
-- ---------------------------------------------------------------------
CREATE TABLE pedido (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id    INT           NOT NULL,
    direccion_id  INT           NOT NULL,
    fecha         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado        VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE_PAGO',
    total         DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT ck_pedido_estado CHECK (estado IN ('PENDIENTE_PAGO', 'PAGADO', 'CANCELADO')),
    CONSTRAINT ck_pedido_total CHECK (total >= 0),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_pedido_direccion FOREIGN KEY (direccion_id) REFERENCES direccion (id)
);

-- Detalle del pedido: conserva el precio aplicado aunque después cambie el catálogo.
CREATE TABLE linea_pedido (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id        INT           NOT NULL,
    producto_id      INT           NOT NULL,
    cantidad         INT           NOT NULL,
    precio_unitario  DECIMAL(10,2) NOT NULL,
    importe          DECIMAL(10,2) NOT NULL,
    CONSTRAINT uk_linea_pedido_producto UNIQUE (pedido_id, producto_id),
    CONSTRAINT ck_linea_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_linea_precio CHECK (precio_unitario >= 0),
    CONSTRAINT ck_linea_importe CHECK (importe >= 0),
    CONSTRAINT fk_linea_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_linea_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

-- Cada cambio de stock queda explicado. pedido_id es NULL en las reposiciones.
CREATE TABLE movimiento_stock (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    producto_id       INT          NOT NULL,
    tipo              VARCHAR(12)  NOT NULL,
    cantidad          INT          NOT NULL,
    stock_resultante  INT          NOT NULL,
    pedido_id         INT,
    motivo            VARCHAR(150),
    fecha             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA', 'DEVOLUCION')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_movimiento_resultante CHECK (stock_resultante >= 0),
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_movimiento_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
);

-- Relación 1:1 con pedido: UNIQUE en pedido_id impide dos pagos para el mismo pedido.
CREATE TABLE pago (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id   INT           NOT NULL,
    importe     DECIMAL(10,2) NOT NULL,
    metodo      VARCHAR(15)   NOT NULL,
    estado      VARCHAR(12)   NOT NULL,
    referencia  VARCHAR(40)   NOT NULL,
    fecha       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pago_pedido UNIQUE (pedido_id),
    CONSTRAINT uk_pago_referencia UNIQUE (referencia),
    CONSTRAINT ck_pago_importe CHECK (importe > 0),
    CONSTRAINT ck_pago_metodo CHECK (metodo IN ('TARJETA', 'BIZUM', 'TRANSFERENCIA')),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('COMPLETADO', 'REEMBOLSADO')),
    CONSTRAINT fk_pago_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
);
