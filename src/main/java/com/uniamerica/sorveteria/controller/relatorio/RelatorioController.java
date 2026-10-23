
package com.uniamerica.sorveteria.controller.relatorio;

import com.uniamerica.sorveteria.controller.relatorio.dto.FaturamentoDiarioResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.RelatorioAnualResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.ResumoVendasResponse;
import com.uniamerica.sorveteria.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

  private final RelatorioService relatorioService;

  @GetMapping("/vendas/resumo")
  public ResumoVendasResponse gerarResumoMensal(
    @RequestParam(required = false) Integer mes,
    @RequestParam(required = false) Integer ano) {

    LocalDate hoje = LocalDate.now();

    int mesConsulta = mes != null ? mes : hoje.getMonthValue();
    int anoConsulta = ano != null ? ano : hoje.getYear();

    return relatorioService.gerarResumoMensal(mesConsulta, anoConsulta);
  }

  @GetMapping("/vendas/diario")
  public FaturamentoDiarioResponse gerarFaturamentoDiario(
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate data) {

    return relatorioService.gerarFaturamentoDiario(data);
  }


  @GetMapping("/vendas/anual")
  public RelatorioAnualResponse gerarRelatorioAnual(
    @RequestParam int ano) {

    return relatorioService.gerarRelatorioAnual(ano);
  }

}
