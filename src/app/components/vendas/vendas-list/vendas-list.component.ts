import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Venda } from '../../../models/venda';
import { VendaService } from '../../../services/venda.service';
import { AuthService } from '../../../services/auth.service';
import { mostrarErro } from '../../../utils/erro';

@Component({
  selector: 'app-vendas-list',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './vendas-list.component.html',
  styleUrl: './vendas-list.component.scss'
})
export class VendasListComponent implements OnInit {

  lista: Venda[] = [];
  carregando = true;

  vendaService = inject(VendaService);
  authService = inject(AuthService);

  ngOnInit(): void {
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.vendaService.findAll().subscribe({
      next: vendas => {
        this.lista = [...vendas].sort((a, b) => b.id - a.id);
        this.carregando = false;
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando = false;
        mostrarErro(erro, 'Erro ao carregar vendas');
      }
    });
  }

  resumoItens(venda: Venda): string {
    return venda.itens.map(item => `${item.quantidade}x ${item.produtoNome}`).join(', ');
  }

  cancelar(venda: Venda) {
    this.confirmar(`Cancelar a venda #${venda.id}?`, 'O estoque dos produtos será devolvido.', 'Cancelar venda').then(ok => {
      if (!ok) {
        return;
      }
      this.vendaService.cancelar(venda.id).subscribe({
        next: () => {
          Swal.fire({ title: 'Venda cancelada', icon: 'success' });
          this.carregar();
        },
        error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Erro ao cancelar venda')
      });
    });
  }

  deletar(venda: Venda) {
    this.confirmar(`Excluir a venda #${venda.id}?`, 'Essa ação não pode ser desfeita.', 'Excluir').then(ok => {
      if (!ok) {
        return;
      }
      this.vendaService.delete(venda.id).subscribe({
        next: () => {
          Swal.fire({ title: 'Venda excluída', icon: 'success' });
          this.carregar();
        },
        error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Erro ao excluir venda')
      });
    });
  }

  private confirmar(titulo: string, texto: string, botao: string): Promise<boolean> {
    return Swal.fire({
      title: titulo,
      text: texto,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: botao,
      cancelButtonText: 'Voltar'
    }).then(resultado => resultado.isConfirmed);
  }
}
