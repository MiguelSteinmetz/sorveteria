
package com.uniamerica.sorveteria.controller.relatorio.dto;

import java.time.LocalDate;

public record FaturamentoDiarioResponse(
  LocalDate data,
  Double faturamento,
  Long quantidadeVendas
) {}
