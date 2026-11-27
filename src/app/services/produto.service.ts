import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Produto } from '../models/produto';

@Injectable({ providedIn: 'root' })
export class ProdutoService {

  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/api/produtos`;

  findAll(): Observable<Produto[]> {
    return this.http.get<Produto[]>(this.url);
  }

  findDisponiveis(): Observable<Produto[]> {
    return this.http.get<Produto[]>(`${this.url}/venda`);
  }

  findById(id: number): Observable<Produto> {
    return this.http.get<Produto>(`${this.url}/${id}`);
  }

  save(produto: Produto): Observable<Produto> {
    const { id, ...dados } = produto;
    return this.http.post<Produto>(this.url, dados);
  }

  update(produto: Produto): Observable<Produto> {
    const { id, ...dados } = produto;
    return this.http.put<Produto>(`${this.url}/${id}`, dados);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
