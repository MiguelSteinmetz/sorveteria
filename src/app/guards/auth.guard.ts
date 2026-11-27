import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.estaLogado() ? true : router.createUrlTree(['/login']);
};

export const gerenteGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.estaLogado()) {
    return router.createUrlTree(['/login']);
  }
  return authService.ehGerente() ? true : router.createUrlTree(['/admin/produtos']);
};

export const loginGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.estaLogado() ? router.createUrlTree(['/admin/produtos']) : true;
};
