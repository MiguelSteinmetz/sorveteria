import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { FaturamentoDiario, RelatorioAnual, ResumoVendas } from '../../models/relatorio';
import { RelatorioService } from '../../services/relatorio.service';
import { mostrarErro } from '../../utils/erro';

@Component({
  selector: 'app-relatorios',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './relatorios.component.html',
  styleUrl: './relatorios.component.scss'
})
export class RelatoriosComponent implements OnInit {

  readonly nomesMeses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

  mes = new Date().getMonth() + 1;
  ano = new Date().getFullYear();
  data = this.hojeLocal();

  resumo: ResumoVendas | null = null;
  diario: FaturamentoDiario | null = null;
  anual: RelatorioAnual | null = null;

  relatorioService = inject(RelatorioService);

  ngOnInit(): void {
    this.buscarResumo();
    this.buscarDiario();
    this.buscarAnual();
  }

  buscarResumo() {
    this.relatorioService.resumoMensal(this.mes, this.ano).subscribe({
      next: resumo => this.resumo = resumo,
      error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Erro ao gerar resumo mensal')
    });
  }

  buscarDiario() {
    if (!this.data) {
      return;
    }
    this.relatorioService.diario(this.data).subscribe({
      next: diario => this.diario = diario,
      error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Erro ao gerar faturamento diário')
    });
  }

  buscarAnual() {
    this.relatorioService.anual(this.ano).subscribe({
      next: anual => this.anual = anual,
      error: (erro: HttpErrorResponse) => mostrarErro(erro, 'Erro ao gerar relatório anual')
    });
  }

  atualizarPeriodo() {
    this.buscarResumo();
    this.buscarAnual();
  }

  private hojeLocal(): string {
    const hoje = new Date();
    const mes = String(hoje.getMonth() + 1).padStart(2, '0');
    const dia = String(hoje.getDate()).padStart(2, '0');
    return `${hoje.getFullYear()}-${mes}-${dia}`;
  }

  larguraBarra(faturamento: number): number {
    const maior = Math.max(...(this.anual?.meses.map(m => m.faturamento ?? 0) ?? [0]));
    return maior > 0 ? ((faturamento ?? 0) / maior) * 100 : 0;
  }
}
