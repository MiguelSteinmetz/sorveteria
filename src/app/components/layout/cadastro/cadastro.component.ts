import { Component, inject } from '@angular/core';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { AuthService } from '../../../services/auth.service';
import { CadastroRequest } from '../../../models/login';
import { mensagemDeErro } from '../../../utils/erro';

@Component({
  selector: 'app-cadastro',
  imports: [MdbFormsModule, FormsModule, RouterLink],
  templateUrl: './cadastro.component.html',
  styleUrl: './cadastro.component.scss'
})
export class CadastroComponent {

  dados: CadastroRequest = { nome: '', login: '', senha: '', cep: '' };
  confirmacao = '';
  carregando = false;

  router = inject(Router);
  authService = inject(AuthService);

  cadastrar() {
    const problema = this.validar();
    if (problema) {
      Swal.fire({ title: 'Verifique os dados', text: problema, icon: 'warning' });
      return;
    }

    this.carregando = true;
    const dados = { ...this.dados, cep: this.dados.cep.replace(/\D/g, '') };
    this.authService.cadastrar(dados).subscribe({
      next: () => {
        this.carregando = false;
        Swal.fire({ title: 'Conta criada!', text: 'Agora é só fazer login.', icon: 'success' });
        this.router.navigate(['/login']);
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando = false;
        Swal.fire({ title: 'Não foi possível cadastrar', text: mensagemDeErro(erro), icon: 'error' });
      }
    });
  }

  private validar(): string | null {
    if (!this.dados.nome.trim() || !this.dados.login.trim() || !this.dados.senha || !this.dados.cep) {
      return 'Preencha todos os campos.';
    }
    if (this.dados.senha.length < 6) {
      return 'A senha deve ter pelo menos 6 caracteres.';
    }
    if (this.dados.senha !== this.confirmacao) {
      return 'As senhas não conferem.';
    }
    if (this.dados.cep.replace(/\D/g, '').length !== 8) {
      return 'O CEP deve conter 8 números.';
    }
    return null;
  }
}
