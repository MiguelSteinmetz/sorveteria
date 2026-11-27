import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Produto } from '../../../models/produto';
import { FormaPagamento } from '../../../models/venda';
import { ProdutoService } from '../../../services/produto.service';
import { VendaService } from '../../../services/venda.service';
import { mostrarErro } from '../../../utils/erro';

interface ItemCarrinho {
  produto: Produto;
  quantidade: number;
}

const TAMANHO_QR = 25;

@Component({
  selector: 'app-venda-form',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './venda-form.component.html',
  styleUrl: './venda-form.component.scss'
})
export class VendaFormComponent implements OnInit {

  readonly formasPagamento: { valor: FormaPagamento; nome: string; icone: string }[] = [
    { valor: 'DINHEIRO', nome: 'Dinheiro', icone: 'fas fa-money-bill-wave' },
    { valor: 'DEBITO', nome: 'Cartão de débito', icone: 'fas fa-credit-card' },
    { valor: 'CREDITO', nome: 'Cartão de crédito', icone: 'far fa-credit-card' },
    { valor: 'PIX', nome: 'Pix', icone: 'fas fa-qrcode' }
  ];
  readonly tamanhoQr = TAMANHO_QR;
  readonly modulosQr = this.gerarQrExemplo();

  produtos: Produto[] = [];
  carrinho: ItemCarrinho[] = [];
  produtoSelecionadoId: number | null = null;
  quantidade = 1;
  pagando = false;
  formaPagamento: FormaPagamento | null = null;
  valorRecebido: number | null = null;
  salvando = false;

  produtoService = inject(ProdutoService);
  vendaService = inject(VendaService);
  router = inject(Router);

  ngOnInit(): void {
    this.produtoService.findDisponiveis().subscribe({
      next: produtos => this.produtos = produtos,
      error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Erro ao carregar produtos')
    });
  }

  get total(): number {
    return this.carrinho.reduce((soma, item) => soma + item.produto.preco * item.quantidade, 0);
  }

  get troco(): number {
    return (this.valorRecebido ?? 0) - this.total;
  }

  adicionar() {
    const produto = this.produtos.find(p => p.id === Number(this.produtoSelecionadoId));
    if (!produto) {
      Swal.fire({ title: 'Selecione um produto', icon: 'warning' });
      return;
    }
    if (!this.quantidade || this.quantidade < 1) {
      Swal.fire({ title: 'Informe uma quantidade válida', icon: 'warning' });
      return;
    }

    const existente = this.carrinho.find(item => item.produto.id === produto.id);
    const quantidadeFinal = (existente?.quantidade ?? 0) + this.quantidade;
    if (quantidadeFinal > produto.estoque) {
      Swal.fire({ title: 'Estoque insuficiente', text: `Disponível: ${produto.estoque}`, icon: 'warning' });
      return;
    }

    if (existente) {
      existente.quantidade = quantidadeFinal;
    } else {
      this.carrinho.push({ produto, quantidade: this.quantidade });
    }
    this.produtoSelecionadoId = null;
    this.quantidade = 1;
  }

  remover(item: ItemCarrinho) {
    this.carrinho = this.carrinho.filter(i => i !== item);
  }

  irParaPagamento() {
    if (this.carrinho.length === 0) {
      Swal.fire({ title: 'Adicione pelo menos um item', icon: 'warning' });
      return;
    }
    this.pagando = true;
  }

  voltarParaItens() {
    this.pagando = false;
    this.formaPagamento = null;
    this.valorRecebido = null;
  }

  escolher(forma: FormaPagamento) {
    this.formaPagamento = forma;
    this.valorRecebido = null;
  }

  confirmar() {
    if (!this.formaPagamento) {
      Swal.fire({ title: 'Escolha a forma de pagamento', icon: 'warning' });
      return;
    }
    if (this.formaPagamento === 'DINHEIRO' && this.troco < 0) {
      Swal.fire({ title: 'Valor recebido insuficiente', text: 'Informe um valor igual ou maior que o total.', icon: 'warning' });
      return;
    }

    this.salvando = true;
    const itens = this.carrinho.map(item => ({ produtoId: item.produto.id, quantidade: item.quantidade }));
    this.vendaService.registrar({ itens, formaPagamento: this.formaPagamento }).subscribe({
      next: venda => {
        this.salvando = false;
        Swal.fire({ title: `Venda #${venda.id} registrada!`, icon: 'success' });
        this.router.navigate(['/admin/vendas']);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando = false;
        mostrarErro(erro, 'Erro ao registrar venda');
      }
    });
  }

  private gerarQrExemplo(): { x: number; y: number }[] {
    const modulos: { x: number; y: number }[] = [];
    let semente = 7;
    const aleatorio = () => {
      semente = (semente * 9301 + 49297) % 233280;
      return semente / 233280;
    };
    const ehLocalizador = (x: number, y: number) => {
      const cantos = [[0, 0], [TAMANHO_QR - 7, 0], [0, TAMANHO_QR - 7]];
      return cantos.some(([cx, cy]) => x >= cx - 1 && x <= cx + 7 && y >= cy - 1 && y <= cy + 7);
    };
    const desenhaLocalizador = (x: number, y: number) => {
      const cantos = [[0, 0], [TAMANHO_QR - 7, 0], [0, TAMANHO_QR - 7]];
      return cantos.some(([cx, cy]) => {
        const dx = x - cx;
        const dy = y - cy;
        if (dx < 0 || dx > 6 || dy < 0 || dy > 6) {
          return false;
        }
        const borda = dx === 0 || dx === 6 || dy === 0 || dy === 6;
        const miolo = dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4;
        return borda || miolo;
      });
    };

    for (let y = 0; y < TAMANHO_QR; y++) {
      for (let x = 0; x < TAMANHO_QR; x++) {
        const preenchido = ehLocalizador(x, y) ? desenhaLocalizador(x, y) : aleatorio() > 0.5;
        if (preenchido) {
          modulos.push({ x, y });
        }
      }
    }
    return modulos;
  }
}
