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

## Front-end (Angular)

```
npm install
npm start
```

Abre em http://localhost:4200 e usa o `proxy.conf.json` para chamar a API em localhost:8080.

## Deploy na AWS (EC2)

O deploy é feito pelo script `deploy/provision-ec2.sh` numa EC2 Ubuntu 24.04 (t2.micro ou t3.micro).
Ele instala Nginx, PostgreSQL, Java 17, Maven e Node 20, cria o banco, gera senha do banco e `JWT_SECRET`
aleatórios em `/etc/sorveteria/sorveteria.env` (fora do Git), faz o build do back e do front e sobe tudo.

1. Security Group: liberar só 22 (SSH, seu IP), 80 e 443. A porta 5432 fica fechada.
2. Conectar na instância e rodar:

```
curl -fsSL https://raw.githubusercontent.com/MiguelSteinmetz/sorveteria/master/deploy/provision-ec2.sh -o provision.sh
sudo bash provision.sh
```

O script pergunta o login e a senha do primeiro GERENTE. No final mostra o IP público para acessar.

- Spring Boot roda em segundo plano pelo systemd (`deploy/sorveteria.service`), só em localhost:8080.
- Nginx serve o build do Angular e faz proxy de `/api` (`deploy/nginx-sorveteria.conf`), com fallback das rotas para o `index.html`.
- PostgreSQL só escuta em localhost.

Logs e status:

```
systemctl status sorveteria
journalctl -u sorveteria -f
```

Mais detalhes em `deploy/README.md`.
