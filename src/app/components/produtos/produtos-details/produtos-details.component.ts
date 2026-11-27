import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Produto } from '../../../models/produto';
import { ProdutoService } from '../../../services/produto.service';
import { mostrarErro } from '../../../utils/erro';

@Component({
  selector: 'app-produtos-details',
  imports: [MdbFormsModule, FormsModule, RouterLink],
  templateUrl: './produtos-details.component.html',
  styleUrl: './produtos-details.component.scss'
})
export class ProdutosDetailsComponent {

  produto = new Produto(0, '', 'SORVETE', 0, 0, true);
  categorias = ['SORVETE', 'CASQUINHA', 'ACAI', 'MILK_SHAKE', 'ADICIONAL'];
  salvando = false;

  rotaAtiva = inject(ActivatedRoute);
  router = inject(Router);
  produtoService = inject(ProdutoService);

  constructor() {
    const id = Number(this.rotaAtiva.snapshot.params['id']);
    if (id > 0) {
      this.findById(id);
    }
  }

  get editando(): boolean {
    return this.produto.id > 0;
  }

  findById(id: number) {
    this.produtoService.findById(id).subscribe({
      next: produto => this.produto = produto,
      error: (erro: HttpErrorResponse) => {
        mostrarErro(erro, 'Produto não encontrado');
        this.router.navigate(['/admin/produtos']);
      }
    });
  }

  salvar() {
    const requisicao = this.editando
      ? this.produtoService.update(this.produto)
      : this.produtoService.save(this.produto);

    this.salvando = true;
    requisicao.subscribe({
      next: () => {
        this.salvando = false;
        Swal.fire({ title: this.editando ? 'Editado com sucesso!' : 'Salvo com sucesso!', icon: 'success' });
        this.router.navigate(['/admin/produtos']);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando = false;
        mostrarErro(erro, 'Verifique os dados');
      }
    });
  }
}
