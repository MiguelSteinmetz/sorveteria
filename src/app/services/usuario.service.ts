import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UsuarioAtualizacaoRequest, UsuarioRequest, UsuarioResponse } from '../models/login';

@Injectable({ providedIn: 'root' })
export class UsuarioService {

  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/api/usuarios`;

  listar(): Observable<UsuarioResponse[]> {
    return this.http.get<UsuarioResponse[]>(this.url);
  }

  cadastrar(usuario: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(this.url, usuario);
  }

  atualizar(id: number, usuario: UsuarioAtualizacaoRequest): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.url}/${id}`, usuario);
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
