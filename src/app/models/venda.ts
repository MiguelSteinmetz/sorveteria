export type StatusVenda = 'CONCLUIDA' | 'CANCELADA';

export interface ItemVendaRequest {
  produtoId: number;
  quantidade: number;
}

export interface VendaRequest {
  itens: ItemVendaRequest[];
}

export interface ItemVenda {
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  valorUnitario: number;
  subtotal: number;
}

export interface Venda {
  id: number;
  itens: ItemVenda[];
  valorTotal: number;
  dataHora: string;
  status: StatusVenda;
}
