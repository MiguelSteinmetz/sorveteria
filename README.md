# ApiSorveteria

API REST do caixa da sorveteria (Spring Boot 4, Java 17, PostgreSQL).

## Configuração (sem credenciais no código)

Todas as credenciais vêm de variáveis de ambiente. Nada sensível fica no código nem no Git.

1. Copie `.env.example` para `.env` na raiz do projeto (o `.env` está no `.gitignore`).
2. Preencha:
   - `DB_PASSWORD`: senha do PostgreSQL
   - `JWT_SECRET`: no mínimo 32 caracteres aleatórios (`openssl rand -base64 48`)
   - `ADMIN_LOGIN` e `ADMIN_PASSWORD`: primeiro usuário GERENTE, criado só se não houver nenhum usuário
3. Rode: `mvn spring-boot:run` ou pela IDE. O Spring lê o `.env` automaticamente.

Se preferir, defina as mesmas variáveis no sistema ou na configuração de execução da IDE.

## Autenticação (JWT)

```
POST /api/auth/login
{ "login": "admin", "senha": "..." }
```

Resposta:

```
{ "token": "eyJ...", "tipo": "Bearer", "expiraEm": "2026-11-25T20:00:00Z", "nome": "Administrador", "cargo": "GERENTE" }
```

Envie o token em todas as outras requisições:

```
Authorization: Bearer eyJ...
```

- O token expira após `JWT_EXPIRATION` (padrão `2h`).
- Sem token, com token inválido ou expirado: **401**.
- Sem permissão para o recurso: **403**.

| Rota | Quem pode |
|---|---|
| `POST /api/auth/login`, Swagger (`/swagger-ui.html`) | público |
| `GET/POST/PUT/PATCH` de produtos e vendas, `GET /api/usuarios/{id}/endereco` | GERENTE e FUNCIONARIO |
| `/api/relatorios/**`, `POST /api/usuarios`, qualquer `DELETE` | só GERENTE |

As senhas são salvas com BCrypt. A migration `V3__criptografar_senhas.sql` converte as senhas antigas em texto puro (usa a extensão `pgcrypto` do PostgreSQL).

## Testes

```
mvn test
```

`ApplicationTests` sobe o contexto completo e precisa do PostgreSQL e das variáveis `DB_PASSWORD` e `JWT_SECRET`.
