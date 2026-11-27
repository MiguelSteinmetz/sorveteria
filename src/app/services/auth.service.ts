import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { CadastroRequest, Cargo, LoginRequest, LoginResponse, UsuarioResponse } from '../models/login';

const CHAVE_TOKEN = 'sorveteria.token';
const CHAVE_USUARIO = 'sorveteria.usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private http = inject(HttpClient);
  private router = inject(Router);
  private url = `${environment.apiUrl}/api/auth`;

  login(dados: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.url}/login`, dados).pipe(
      tap(resposta => {
        localStorage.setItem(CHAVE_TOKEN, resposta.token);
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify({ nome: resposta.nome, cargo: resposta.cargo }));
      })
    );
  }

  cadastrar(dados: CadastroRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${this.url}/cadastro`, dados);
  }

  logout(): void {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    const token = localStorage.getItem(CHAVE_TOKEN);
    if (!token) {
      return null;
    }
    if (this.estaExpirado(token)) {
      localStorage.removeItem(CHAVE_TOKEN);
      localStorage.removeItem(CHAVE_USUARIO);
      return null;
    }
    return token;
  }

  estaLogado(): boolean {
    return this.getToken() !== null;
  }

  getCargo(): Cargo | null {
    if (!this.estaLogado()) {
      return null;
    }
    const usuario = localStorage.getItem(CHAVE_USUARIO);
    return usuario ? JSON.parse(usuario).cargo : null;
  }

  ehGerente(): boolean {
    return this.getCargo() === 'GERENTE';
  }

  getNome(): string | null {
    const usuario = localStorage.getItem(CHAVE_USUARIO);
    return usuario ? JSON.parse(usuario).nome : null;
  }

  private estaExpirado(token: string): boolean {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64));
      return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
