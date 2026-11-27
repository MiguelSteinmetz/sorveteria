import { Routes } from '@angular/router';
import { LoginComponent } from './components/layout/login/login.component';
import { CadastroComponent } from './components/layout/cadastro/cadastro.component';
import { PrincipalComponent } from './components/layout/principal/principal.component';
import { ProdutosListComponent } from './components/produtos/produtos-list/produtos-list.component';
import { ProdutosDetailsComponent } from './components/produtos/produtos-details/produtos-details.component';
import { VendasListComponent } from './components/vendas/vendas-list/vendas-list.component';
import { VendaFormComponent } from './components/vendas/venda-form/venda-form.component';
import { RelatoriosComponent } from './components/relatorios/relatorios.component';
import { authGuard, gerenteGuard, loginGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, canActivate: [loginGuard] },
  { path: 'cadastro', component: CadastroComponent, canActivate: [loginGuard] },
  {
    path: 'admin',
    component: PrincipalComponent,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: '', redirectTo: 'produtos', pathMatch: 'full' },
      { path: 'produtos', component: ProdutosListComponent },
      { path: 'produtos/new', component: ProdutosDetailsComponent },
      { path: 'produtos/edit/:id', component: ProdutosDetailsComponent },
      { path: 'vendas', component: VendasListComponent },
      { path: 'vendas/nova', component: VendaFormComponent },
      { path: 'relatorios', component: RelatoriosComponent, canActivate: [gerenteGuard] }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
