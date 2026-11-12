package com.carloscaceres.tienda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Actividad de un cliente calculada en SQL (COUNT, SUM, MAX); no corresponde a ninguna tabla. */
public record ResumenClienteDto(int clienteId, String cliente, String email, int numeroPedidos,
                                int pedidosCancelados, BigDecimal totalGastado, LocalDateTime ultimoPedido) {
}
