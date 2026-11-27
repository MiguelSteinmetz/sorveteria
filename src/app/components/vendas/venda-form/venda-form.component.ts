import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Produto } from '../../../models/produto';
import { ProdutoService } from '../../../services/produto.service';
import { VendaService } from '../../../services/venda.service';
import { mostrarErro } from '../../../utils/erro';

interface ItemCarrinho {
  produto: Produto;
  quantidade: number;
}

@Component({
  selector: 'app-venda-form',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './venda-form.component.html',
  styleUrl: './venda-form.component.scss'
})
export class VendaFormComponent implements OnInit {

  produtos: Produto[] = [];
  carrinho: ItemCarrinho[] = [];
  produtoSelecionadoId: number | null = null;
  quantidade = 1;
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

  finalizar() {
    if (this.carrinho.length === 0) {
      Swal.fire({ title: 'Adicione pelo menos um item', icon: 'warning' });
      return;
    }

    this.salvando = true;
    const itens = this.carrinho.map(item => ({ produtoId: item.produto.id, quantidade: item.quantidade }));
    this.vendaService.registrar({ itens }).subscribe({
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
}
