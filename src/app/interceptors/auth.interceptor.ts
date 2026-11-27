import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import Swal from 'sweetalert2';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

const ROTAS_PUBLICAS = ['/api/auth/login', '/api/auth/cadastro'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const ehRequisicaoDaApi = req.url.startsWith(`${environment.apiUrl}/api/`);
  const ehRotaPublica = ROTAS_PUBLICAS.some(rota => req.url.endsWith(rota));
  const token = authService.getToken();

  if (ehRequisicaoDaApi && !ehRotaPublica && token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(req).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (ehRequisicaoDaApi && !ehRotaPublica) {
        if (erro.status === 401) {
          authService.logout();
          Swal.fire({ title: 'Sessão expirada', text: 'Faça login novamente.', icon: 'warning' });
        } else if (erro.status === 403) {
          Swal.fire({ title: 'Acesso negado', text: 'Você não tem permissão para esta ação.', icon: 'error' });
        }
      }
      return throwError(() => erro);
    })
  );
};
