import { Component, inject } from '@angular/core';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { AuthService } from '../../../services/auth.service';
import { mensagemDeErro } from '../../../utils/erro';

@Component({
  selector: 'app-login',
  imports: [MdbFormsModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  usuario = '';
  senha = '';
  carregando = false;

  router = inject(Router);
  authService = inject(AuthService);

  logar() {
    if (!this.usuario || !this.senha) {
      Swal.fire({ title: 'Informe usuário e senha', icon: 'warning' });
      return;
    }

    this.carregando = true;
    this.authService.login({ login: this.usuario, senha: this.senha }).subscribe({
      next: () => {
        this.carregando = false;
        this.router.navigate(['/admin/produtos']);
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando = false;
        const mensagem = erro.status === 401 ? 'Usuário ou senha incorretos' : mensagemDeErro(erro);
        Swal.fire({ title: mensagem, icon: 'error' });
      }
    });
  }
}
