import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2';

export function mensagemDeErro(erro: HttpErrorResponse, padrao = 'Não foi possível concluir a operação'): string {
  if (erro.status === 0) {
    return 'Servidor indisponível. Tente novamente em instantes.';
  }
  const corpo = erro.error;
  if (corpo?.campos) {
    return Object.values(corpo.campos).join('\n');
  }
  if (typeof corpo?.mensagem === 'string' && corpo.mensagem) {
    return corpo.mensagem;
  }
  return padrao;
}

export function mostrarErro(erro: HttpErrorResponse, titulo = 'Ops!'): void {
  if (erro.status === 401 || erro.status === 403) {
    return;
  }
  Swal.fire({ title: titulo, text: mensagemDeErro(erro), icon: 'error' });
}
