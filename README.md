# Sorveteria - Front-end

Front-end em Angular 19 da API Sorveteria.

## Funcionalidades

- Login com JWT e cadastro de novos usuários
- Produtos: listagem, cadastro, edição e exclusão (exclusão só para GERENTE)
- Vendas: nova venda com vários itens, listagem, cancelamento e exclusão
- Relatórios de vendas (só GERENTE): resumo do mês, faturamento por mês do ano e faturamento do dia

## Segurança

- `src/app/interceptors/auth.interceptor.ts` envia o token em `Authorization: Bearer` em toda chamada à API
- `src/app/guards/auth.guard.ts` protege as telas restritas (`authGuard`, `gerenteGuard`, `loginGuard`)
- Resposta 401 faz logout automático; 403 mostra "Acesso negado"

## Endereço da API

O código chama a API por caminhos relativos (`/api/...`), sem `localhost` fixo:

- Em desenvolvimento, o `ng serve` usa o `proxy.conf.json`, que encaminha `/api` para `http://localhost:8080`
- Em produção, o Nginx serve o build e encaminha `/api` para o Spring Boot

Se a API ficar em outro domínio, altere `apiUrl` em `src/environments/environment.ts` e libere esse domínio no CORS do back-end (`CORS_ALLOWED_ORIGINS`).

## Rodando localmente

Com a API rodando na porta 8080:

```bash
npm ci
npm start
```

Acesse http://localhost:4200

## Build de produção

```bash
npm run build
```

Os arquivos ficam em `dist/sorveteria_front_end/browser`.
