export interface ResumoVendas {
  faturamento: number;
  quantidadeVendas: number;
  ticketMedio: number;
  vendasCanceladas: number;
}

export interface FaturamentoDiario {
  data: string;
  faturamento: number;
  quantidadeVendas: number;
}

export interface ResumoMensal {
  mes: number;
  faturamento: number;
  quantidadeVendas: number;
}

export interface RelatorioAnual {
  ano: number;
  faturamentoTotal: number;
  quantidadeVendasTotal: number;
  meses: ResumoMensal[];
}
