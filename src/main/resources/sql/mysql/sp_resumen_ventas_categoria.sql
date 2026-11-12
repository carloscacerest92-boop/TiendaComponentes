-- =====================================================================
-- Procedimiento almacenado ESPECÍFICO DE MYSQL.
-- Resumen de ventas de una categoría: unidades vendidas e ingresos,
-- sin contar pedidos cancelados.
--   IN  p_categoria : nombre de la categoría
--   OUT p_unidades  : unidades vendidas
--   OUT p_ingresos  : importe total vendido
-- Se instala desde Java como UNA sola sentencia (sin DELIMITER, que es
-- una directiva del cliente MySQL Workbench, no del servidor).
-- =====================================================================
CREATE PROCEDURE sp_resumen_ventas_categoria(
    IN  p_categoria VARCHAR(60),
    OUT p_unidades  INT,
    OUT p_ingresos  DECIMAL(12,2))
BEGIN
    SELECT COALESCE(SUM(l.cantidad), 0),
           COALESCE(SUM(l.importe), 0)
    INTO p_unidades, p_ingresos
    FROM linea_pedido l
    JOIN pedido p ON p.id = l.pedido_id
    JOIN producto_categoria pc ON pc.producto_id = l.producto_id
    JOIN categoria c ON c.id = pc.categoria_id
    WHERE c.nombre = p_categoria
      AND p.estado <> 'CANCELADO';
END
