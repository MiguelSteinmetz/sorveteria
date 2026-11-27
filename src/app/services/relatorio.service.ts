import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FaturamentoDiario, RelatorioAnual, ResumoVendas } from '../models/relatorio';

@Injectable({ providedIn: 'root' })
export class RelatorioService {

  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/api/relatorios/vendas`;

  resumoMensal(mes: number, ano: number): Observable<ResumoVendas> {
    return this.http.get<ResumoVendas>(`${this.url}/resumo`, { params: { mes, ano } });
  }

  diario(data: string): Observable<FaturamentoDiario> {
    return this.http.get<FaturamentoDiario>(`${this.url}/diario`, { params: { data } });
  }

  anual(ano: number): Observable<RelatorioAnual> {
    return this.http.get<RelatorioAnual>(`${this.url}/anual`, { params: { ano } });
  }
}
