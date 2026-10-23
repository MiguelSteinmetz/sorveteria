
package com.uniamerica.sorveteria.service;

import com.uniamerica.sorveteria.controller.relatorio.dto.FaturamentoDiarioResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.RelatorioAnualResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.ResumoVendasResponse;
import com.uniamerica.sorveteria.entity.StatusVenda;
import com.uniamerica.sorveteria.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RelatorioService {

  private final VendaRepository vendaRepository;

  public ResumoVendasResponse gerarResumoMensal(int mes, int ano) {
    LocalDate primeiroDia = LocalDate.of(ano, mes, 1);

    LocalDateTime inicio = primeiroDia.atStartOfDay();
    LocalDateTime fim = primeiroDia.plusMonths(1).atStartOfDay();

    Double faturamento = vendaRepository.somarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicio, fim
    );

    Long quantidadeVendas = vendaRepository.contarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicio, fim
    );

    Long vendasCanceladas = vendaRepository.contarVendasPorPeriodo(
      StatusVenda.CANCELADA, inicio, fim
    );

    double ticketMedio = quantidadeVendas == 0
      ? 0.0
      : faturamento / quantidadeVendas;

    return new ResumoVendasResponse(
      faturamento,
      quantidadeVendas,
      ticketMedio,
      vendasCanceladas
    );
  }

  public FaturamentoDiarioResponse gerarFaturamentoDiario(LocalDate data) {
    LocalDateTime inicio = data.atStartOfDay();
    LocalDateTime fim = data.plusDays(1).atStartOfDay();

    Double faturamento = vendaRepository.somarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicio, fim
    );

    Long quantidadeVendas = vendaRepository.contarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicio, fim
    );

    return new FaturamentoDiarioResponse(
      data,
      faturamento,
      quantidadeVendas
    );
  }


  public RelatorioAnualResponse gerarRelatorioAnual(int ano) {
    LocalDate primeiroDia = LocalDate.of(ano, 1, 1);
    LocalDateTime inicioAno = primeiroDia.atStartOfDay();
    LocalDateTime fimAno = primeiroDia.plusYears(1).atStartOfDay();

    Double faturamentoTotal = vendaRepository.somarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicioAno, fimAno
    );

    Long quantidadeVendasTotal = vendaRepository.contarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicioAno, fimAno
    );

    java.util.List<RelatorioAnualResponse.ResumoMensal> meses =
      new java.util.ArrayList<>();

    for (int mes = 1; mes <= 12; mes++) {
      LocalDate inicioMes = LocalDate.of(ano, mes, 1);

      LocalDateTime inicio = inicioMes.atStartOfDay();
      LocalDateTime fim = inicioMes.plusMonths(1).atStartOfDay();

      Double faturamento = vendaRepository.somarVendasPorPeriodo(
        StatusVenda.CONCLUIDA, inicio, fim
      );

      Long quantidadeVendas = vendaRepository.contarVendasPorPeriodo(
        StatusVenda.CONCLUIDA, inicio, fim
      );

      meses.add(new RelatorioAnualResponse.ResumoMensal(
        mes,
        faturamento,
        quantidadeVendas
      ));
    }

    return new RelatorioAnualResponse(
      ano,
      faturamentoTotal,
      quantidadeVendasTotal,
      meses
    );
  }

}
