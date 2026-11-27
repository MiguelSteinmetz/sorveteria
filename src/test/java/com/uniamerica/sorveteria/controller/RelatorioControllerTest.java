
  package com.uniamerica.sorveteria.controller;

import com.uniamerica.sorveteria.controller.relatorio.RelatorioController;
import com.uniamerica.sorveteria.controller.relatorio.dto.FaturamentoDiarioResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.RelatorioAnualResponse;
import com.uniamerica.sorveteria.controller.relatorio.dto.ResumoVendasResponse;
import com.uniamerica.sorveteria.service.RelatorioService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Testa so o controller; as regras de seguranca sao testadas em SecurityConfigTest.
@WebMvcTest(RelatorioController.class)
@AutoConfigureMockMvc(addFilters = false)
class RelatorioControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RelatorioService relatorioService;

  @Test
  void deveRetornarResumoMensalInformadoNaRequisicao() throws Exception {
    ResumoVendasResponse resposta = new ResumoVendasResponse(
      190.5,
      1L,
      190.5,
      2L
    );

    when(relatorioService.gerarResumoMensal(9, 2026))
      .thenReturn(resposta);

    mockMvc.perform(get("/api/relatorios/vendas/resumo")
        .param("mes", "9")
        .param("ano", "2026"))
      .andExpect(status().isOk())
      .andExpect(content().json("""
                        {
                          "faturamento": 190.5,
                          "quantidadeVendas": 1,
                          "ticketMedio": 190.5,
                          "vendasCanceladas": 2
                        }
                        """));

    verify(relatorioService).gerarResumoMensal(9, 2026);
  }

  @Test
  void deveUsarMesEAnoAtuaisQuandoNaoInformados() throws Exception {
    LocalDate hoje = LocalDate.now();

    ResumoVendasResponse resposta = new ResumoVendasResponse(
      0.0,
      0L,
      0.0,
      0L
    );

    when(relatorioService.gerarResumoMensal(
      hoje.getMonthValue(),
      hoje.getYear()
    )).thenReturn(resposta);

    mockMvc.perform(get("/api/relatorios/vendas/resumo"))
      .andExpect(status().isOk())
      .andExpect(content().json("""
                        {
                          "faturamento": 0.0,
                          "quantidadeVendas": 0,
                          "ticketMedio": 0.0,
                          "vendasCanceladas": 0
                        }
                        """));

    verify(relatorioService).gerarResumoMensal(
      hoje.getMonthValue(),
      hoje.getYear()
    );
  }

  @Test
  void deveRetornarFaturamentoDiario() throws Exception {
    LocalDate data = LocalDate.of(2026, 9, 18);

    FaturamentoDiarioResponse resposta =
      new FaturamentoDiarioResponse(
        data,
        190.5,
        1L
      );

    when(relatorioService.gerarFaturamentoDiario(data))
      .thenReturn(resposta);

    mockMvc.perform(get("/api/relatorios/vendas/diario")
        .param("data", "2026-09-18"))
      .andExpect(status().isOk())
      .andExpect(content().json("""
                        {
                          "data": "2026-09-18",
                          "faturamento": 190.5,
                          "quantidadeVendas": 1
                        }
                        """));

    verify(relatorioService).gerarFaturamentoDiario(data);
  }

  @Test
  void deveRetornarErroQuandoDataDiariaForInvalida() throws Exception {
    mockMvc.perform(get("/api/relatorios/vendas/diario")
        .param("data", "data-invalida"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void deveRetornarRelatorioAnual() throws Exception {
    RelatorioAnualResponse resposta = new RelatorioAnualResponse(
      2026,
      190.5,
      1L,
      List.of(
        new RelatorioAnualResponse.ResumoMensal(
          1,
          0.0,
          0L
        ),
        new RelatorioAnualResponse.ResumoMensal(
          9,
          190.5,
          1L
        )
      )
    );

    when(relatorioService.gerarRelatorioAnual(2026))
      .thenReturn(resposta);

    mockMvc.perform(get("/api/relatorios/vendas/anual")
        .param("ano", "2026"))
      .andExpect(status().isOk())
      .andExpect(content().json("""
                        {
                          "ano": 2026,
                          "faturamentoTotal": 190.5,
                          "quantidadeVendasTotal": 1,
                          "meses": [
                            {
                              "mes": 1,
                              "faturamento": 0.0,
                              "quantidadeVendas": 0
                            },
                            {
                              "mes": 9,
                              "faturamento": 190.5,
                              "quantidadeVendas": 1
                            }
                          ]
                        }
                        """));

    verify(relatorioService).gerarRelatorioAnual(2026);
  }

  @Test
  void deveRetornarErroQuandoAnoAnualNaoForInformado() throws Exception {
    mockMvc.perform(get("/api/relatorios/vendas/anual"))
      .andExpect(status().isBadRequest());
  }
}
