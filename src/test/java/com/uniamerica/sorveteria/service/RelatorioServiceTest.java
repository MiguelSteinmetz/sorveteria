
package com.uniamerica.sorveteria.service;

import com.uniamerica.sorveteria.controller.relatorio.dto.FaturamentoDiarioResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.RelatorioAnualResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.ResumoVendasResponse;
import com.uniamerica.sorveteria.entity.StatusVenda;
import com.uniamerica.sorveteria.repository.VendaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

  @Mock
  private VendaRepository vendaRepository;

  @InjectMocks
  private RelatorioService relatorioService;

  @Test
  void deveGerarResumoMensalCorretamente() {
    LocalDateTime inicio = LocalDate.of(2026, 9, 1).atStartOfDay();
    LocalDateTime fim = LocalDate.of(2026, 10, 1).atStartOfDay();

    when(vendaRepository.somarVendasPorPeriodo(
      eq(StatusVenda.CONCLUIDA), eq(inicio), eq(fim)
    )).thenReturn(190.5);

    when(vendaRepository.contarVendasPorPeriodo(
      eq(StatusVenda.CONCLUIDA), eq(inicio), eq(fim)
    )).thenReturn(1L);

    when(vendaRepository.contarVendasPorPeriodo(
      eq(StatusVenda.CANCELADA), eq(inicio), eq(fim)
    )).thenReturn(2L);

    ResumoVendasResponse resultado =
      relatorioService.gerarResumoMensal(9, 2026);

    assertEquals(190.5, resultado.faturamento());
    assertEquals(1L, resultado.quantidadeVendas());
    assertEquals(190.5, resultado.ticketMedio());
    assertEquals(2L, resultado.vendasCanceladas());
  }

  @Test
  void deveGerarFaturamentoDiarioCorretamente() {
    LocalDate data = LocalDate.of(2026, 9, 18);
    LocalDateTime inicio = data.atStartOfDay();
    LocalDateTime fim = data.plusDays(1).atStartOfDay();

    when(vendaRepository.somarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicio, fim
    )).thenReturn(190.5);

    when(vendaRepository.contarVendasPorPeriodo(
      StatusVenda.CONCLUIDA, inicio, fim
    )).thenReturn(1L);

    FaturamentoDiarioResponse resultado =
      relatorioService.gerarFaturamentoDiario(data);

    assertEquals(data, resultado.data());
    assertEquals(190.5, resultado.faturamento());
    assertEquals(1L, resultado.quantidadeVendas());
  }

  @Test
  void deveGerarRelatorioAnualComDozeMeses() {
    LocalDateTime inicioAno =
      LocalDate.of(2026, 1, 1).atStartOfDay();
    LocalDateTime fimAno =
      LocalDate.of(2027, 1, 1).atStartOfDay();

    when(vendaRepository.somarVendasPorPeriodo(
      eq(StatusVenda.CONCLUIDA), any(LocalDateTime.class),
      any(LocalDateTime.class)
    )).thenAnswer(invocacao -> {
      LocalDateTime inicio = invocacao.getArgument(1);
      LocalDateTime fim = invocacao.getArgument(2);

      if (inicio.equals(inicioAno) && fim.equals(fimAno)) {
        return 190.5;
      }

      return inicio.getMonthValue() == 9 ? 190.5 : 0.0;
    });

    when(vendaRepository.contarVendasPorPeriodo(
      eq(StatusVenda.CONCLUIDA), any(LocalDateTime.class),
      any(LocalDateTime.class)
    )).thenAnswer(invocacao -> {
      LocalDateTime inicio = invocacao.getArgument(1);
      LocalDateTime fim = invocacao.getArgument(2);

      if (inicio.equals(inicioAno) && fim.equals(fimAno)) {
        return 1L;
      }

      return inicio.getMonthValue() == 9 ? 1L : 0L;
    });

    RelatorioAnualResponse resultado =
      relatorioService.gerarRelatorioAnual(2026);

    assertEquals(2026, resultado.ano());
    assertEquals(190.5, resultado.faturamentoTotal());
    assertEquals(1L, resultado.quantidadeVendasTotal());
    assertEquals(12, resultado.meses().size());

    RelatorioAnualResponse.ResumoMensal setembro =
      resultado.meses().get(8);

    assertEquals(9, setembro.mes());
    assertEquals(190.5, setembro.faturamento());
    assertEquals(1L, setembro.quantidadeVendas());

    RelatorioAnualResponse.ResumoMensal janeiro =
      resultado.meses().get(0);

    assertEquals(1, janeiro.mes());
    assertEquals(0.0, janeiro.faturamento());
    assertEquals(0L, janeiro.quantidadeVendas());
  }

  @Test
  void deveRetornarTicketMedioZeroQuandoNaoExistemVendas() {
    LocalDateTime inicio = LocalDate.of(2026, 10, 1).atStartOfDay();
    LocalDateTime fim = LocalDate.of(2026, 11, 1).atStartOfDay();

    when(vendaRepository.somarVendasPorPeriodo(
      eq(StatusVenda.CONCLUIDA), eq(inicio), eq(fim)
    )).thenReturn(0.0);

    when(vendaRepository.contarVendasPorPeriodo(
      eq(StatusVenda.CONCLUIDA), eq(inicio), eq(fim)
    )).thenReturn(0L);

    when(vendaRepository.contarVendasPorPeriodo(
      eq(StatusVenda.CANCELADA), eq(inicio), eq(fim)
    )).thenReturn(0L);

    ResumoVendasResponse resultado =
      relatorioService.gerarResumoMensal(10, 2026);

    assertEquals(0.0, resultado.faturamento());
    assertEquals(0L, resultado.quantidadeVendas());
    assertEquals(0.0, resultado.ticketMedio());
    assertEquals(0L, resultado.vendasCanceladas());
  }
}
