import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Cargo, UsuarioRequest, UsuarioResponse } from '../../models/login';
import { UsuarioService } from '../../services/usuario.service';
import { AuthService } from '../../services/auth.service';
import { mostrarErro } from '../../utils/erro';

type Modo = 'lista' | 'novo' | 'editar';

@Component({
  selector: 'app-usuarios',
  imports: [FormsModule, MdbFormsModule],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.scss'
})
export class UsuariosComponent implements OnInit {

  lista: UsuarioResponse[] = [];
  carregando = true;
  modo: Modo = 'lista';

  usuario: UsuarioRequest = this.vazio();
  confirmacao = '';
  salvando = false;

  editando: UsuarioResponse | null = null;
  nomeEdicao = '';
  cargoEdicao: Cargo = 'FUNCIONARIO';

  usuarioService = inject(UsuarioService);
  authService = inject(AuthService);

  ngOnInit(): void {
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.usuarioService.listar().subscribe({
      next: lista => {
        this.lista = lista;
        this.carregando = false;
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando = false;
        mostrarErro(erro, 'Não foi possível carregar os usuários');
      }
    });
  }

  ehVoce(usuario: UsuarioResponse): boolean {
    return usuario.login === this.authService.getLogin();
  }

  nomeCargo(cargo: Cargo): string {
    return cargo === 'GERENTE' ? 'Gerente' : 'Funcionário';
  }

  novo() {
    this.usuario = this.vazio();
    this.confirmacao = '';
    this.modo = 'novo';
  }

  editar(usuario: UsuarioResponse) {
    this.editando = usuario;
    this.nomeEdicao = usuario.nome;
    this.cargoEdicao = usuario.cargo;
    this.modo = 'editar';
  }

  voltar() {
    this.modo = 'lista';
    this.editando = null;
  }

  salvar() {
    const problema = this.validar();
    if (problema) {
      Swal.fire({ title: 'Verifique os dados', text: problema, icon: 'warning' });
      return;
    }

    this.salvando = true;
    const dados = { ...this.usuario, cep: this.usuario.cep.replace(/\D/g, '') };
    this.usuarioService.cadastrar(dados).subscribe({
      next: criado => {
        this.salvando = false;
        Swal.fire({ title: 'Usuário cadastrado!', text: `${criado.nome} (${this.nomeCargo(criado.cargo)})`, icon: 'success' });
        this.voltar();
        this.carregar();
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando = false;
        mostrarErro(erro, 'Não foi possível cadastrar');
      }
    });
  }

  salvarEdicao() {
    if (!this.editando) {
      return;
    }
    if (!this.nomeEdicao.trim()) {
      Swal.fire({ title: 'Verifique os dados', text: 'O nome é obrigatório.', icon: 'warning' });
      return;
    }

    this.salvando = true;
    this.usuarioService.atualizar(this.editando.id, { nome: this.nomeEdicao.trim(), cargo: this.cargoEdicao }).subscribe({
      next: atualizado => {
        this.salvando = false;
        Swal.fire({ title: 'Usuário atualizado!', text: `${atualizado.nome} (${this.nomeCargo(atualizado.cargo)})`, icon: 'success' });
        this.voltar();
        this.carregar();
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando = false;
        mostrarErro(erro, 'Não foi possível atualizar');
      }
    });
  }

  excluir(usuario: UsuarioResponse) {
    Swal.fire({
      title: `Excluir ${usuario.nome}?`,
      text: 'Essa ação não pode ser desfeita.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#dc4c64'
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.usuarioService.deletar(usuario.id).subscribe({
        next: () => {
          Swal.fire({ title: 'Usuário excluído!', icon: 'success' });
          this.carregar();
        },
        error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Não foi possível excluir')
      });
    });
  }

  private validar(): string | null {
    if (!this.usuario.nome.trim() || !this.usuario.login.trim() || !this.usuario.senha || !this.usuario.cep) {
      return 'Preencha todos os campos.';
    }
    if (this.usuario.senha.length < 6) {
      return 'A senha deve ter pelo menos 6 caracteres.';
    }
    if (this.usuario.senha !== this.confirmacao) {
      return 'As senhas não conferem.';
    }
    if (this.usuario.cep.replace(/\D/g, '').length !== 8) {
      return 'O CEP deve conter 8 números.';
    }
    return null;
  }

  private vazio(): UsuarioRequest {
    return { nome: '', login: '', senha: '', cargo: 'FUNCIONARIO', cep: '' };
  }
}
