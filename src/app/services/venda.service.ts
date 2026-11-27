import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Venda, VendaRequest } from '../models/venda';

@Injectable({ providedIn: 'root' })
export class VendaService {

  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/api/vendas`;

  findAll(): Observable<Venda[]> {
    return this.http.get<Venda[]>(this.url);
  }

  registrar(venda: VendaRequest): Observable<Venda> {
    return this.http.post<Venda>(this.url, venda);
  }

  cancelar(id: number): Observable<Venda> {
    return this.http.patch<Venda>(`${this.url}/${id}/cancelar`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
