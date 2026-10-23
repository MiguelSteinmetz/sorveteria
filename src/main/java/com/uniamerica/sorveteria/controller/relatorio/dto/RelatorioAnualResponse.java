
package com.uniamerica.sorveteria.controller.relatorio.dto;

import java.util.List;

public record RelatorioAnualResponse(
  int ano,
  Double faturamentoTotal,
  Long quantidadeVendasTotal,
  List<ResumoMensal> meses
) {
  public record ResumoMensal(
    int mes,
    Double faturamento,
    Long quantidadeVendas
  ) {}
}
