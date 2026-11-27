import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Produto } from '../../../models/produto';
import { ProdutoService } from '../../../services/produto.service';
import { AuthService } from '../../../services/auth.service';
import { mostrarErro } from '../../../utils/erro';

@Component({
  selector: 'app-produtos-list',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './produtos-list.component.html',
  styleUrl: './produtos-list.component.scss'
})
export class ProdutosListComponent implements OnInit {

  lista: Produto[] = [];
  carregando = true;

  produtoService = inject(ProdutoService);
  authService = inject(AuthService);

  ngOnInit(): void {
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.produtoService.findAll().subscribe({
      next: produtos => {
        this.lista = produtos;
        this.carregando = false;
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando = false;
        mostrarErro(erro, 'Erro ao carregar produtos');
      }
    });
  }

  deletar(produto: Produto) {
    Swal.fire({
      title: `Excluir ${produto.nome}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar'
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.produtoService.delete(produto.id).subscribe({
        next: () => {
          Swal.fire({ title: 'Excluído com sucesso!', icon: 'success' });
          this.carregar();
        },
        error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Erro ao excluir produto')
      });
    });
  }
}
