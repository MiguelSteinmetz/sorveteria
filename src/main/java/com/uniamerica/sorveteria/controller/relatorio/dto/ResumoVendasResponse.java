
package com.uniamerica.sorveteria.controller.relatorio.dto;

public record ResumoVendasResponse(
  Double faturamento,
  Long quantidadeVendas,
  Double ticketMedio,
  Long vendasCanceladas
) {}
