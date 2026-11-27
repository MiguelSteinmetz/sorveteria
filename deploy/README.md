# Deploy na AWS (EC2) – Sorveteria

Angular + Spring Boot + PostgreSQL em uma única EC2 Ubuntu, provisionada por `provision-ec2.sh`.

```
Internet ──80/443──▶ Nginx ──┬── /            → build Angular (fallback SPA: try_files … /index.html)
                             └── /api/**      → Spring Boot 127.0.0.1:8080 (systemd, JWT)
                                                  └── PostgreSQL 127.0.0.1:5432 (fechado para fora)
```

## Arquivos

| Arquivo | Função |
|---|---|
| `provision-ec2.sh` | Instala Nginx, PostgreSQL, Java 17, Maven, Node 20; cria swap, banco, usuário e `.env` protegido (senha do banco e `JWT_SECRET` aleatórios); faz o build e sobe tudo |
| `sorveteria.service` | Unit do systemd: back-end em segundo plano, reinicia sozinho, sobe no boot |
| `nginx-sorveteria.conf` | Serve o Angular, faz proxy de `/api` e fallback de rotas SPA |
| `aws-security-group.sh` | (CloudShell) cria o Security Group: 22 só do seu IP, 80/443 públicos, **sem 5432** |

## Passo a passo (conta da demonstração ao vivo)

1. **Security Group**: no Console AWS abra o CloudShell e rode
   ```bash
   curl -fsSLO https://raw.githubusercontent.com/MiguelSteinmetz/sorveteria/master/deploy/aws-security-group.sh
   bash aws-security-group.sh $(curl -s https://checkip.amazonaws.com)   # rode o checkip no SEU PC, não no CloudShell
   ```
   Ou crie manualmente: SSH 22 → Meu IP; HTTP 80 → 0.0.0.0/0; HTTPS 443 → 0.0.0.0/0.
2. **EC2**: Launch instance → Ubuntu Server 24.04 LTS → `t2.micro` ou `t3.micro` (free tier) → disco 20 GB → Security Group `sorveteria-sg`.
3. **Conectar**: EC2 → Instância → Connect → EC2 Instance Connect (ou `ssh -i chave.pem ubuntu@IP`).
4. **Rodar o script**:
   ```bash
   curl -fsSL https://raw.githubusercontent.com/MiguelSteinmetz/sorveteria/master/deploy/provision-ec2.sh -o provision.sh
   sudo bash provision.sh
   ```
   Ele pergunta só o login e a senha do primeiro GERENTE (ou passe `ADMIN_LOGIN=... ADMIN_PASSWORD=...`). Leva ~10–15 min no free tier.
5. Ao final o script mostra: API sem token → **401** (JWT funcionando), rota `/login` → **200** (fallback SPA), PostgreSQL só em `127.0.0.1:5432`, e a URL `http://IP-PUBLICO/`.

> No EC2 Instance Connect a origem do SSH é a faixa da AWS, não o seu IP. Se usar ele, libere a 22 temporariamente para o prefixo `EC2_INSTANCE_CONNECT` da região, ou use SSH com chave a partir do seu IP.

## Comandos úteis na demonstração

```bash
systemctl status sorveteria          # back-end rodando em segundo plano
journalctl -u sorveteria -f          # logs do Spring Boot
sudo ss -ltnp | grep -E ':80|:8080|:5432'   # 8080 e 5432 só em 127.0.0.1
curl -i http://localhost/api/produtos       # 401 sem token
sudo bash provision.sh               # re-deploy (idempotente: puxa o código novo e reinicia)
```

## Roteiro da apresentação (critério → o que mostrar)

| Critério | Como provar |
|---|---|
| Script `.sh` funcional, mínima intervenção | Conta 2: rodar `sudo bash provision.sh` ao vivo; só pede login/senha do gerente |
| Acessível via IP público | Abrir `http://IP-PUBLICO/` no navegador e fazer login |
| Back-end em segundo plano (systemd) | `systemctl status sorveteria` (active/running, enabled) |
| Build de produção do Angular + fallback SPA | Abrir `http://IP-PUBLICO/produtos` direto e dar F5: a página carrega, sem 404 |
| JWT | `curl -i http://IP-PUBLICO/api/produtos` → 401; logado no front a mesma rota funciona |
| Security Group seguro, 5432 fechada | Mostrar as regras de entrada (22 só seu IP, 80/443) e `sudo ss -ltnp \| grep 5432` → só 127.0.0.1 |
| Duas contas | Conta 1 já online com o sistema; conta 2 executando o script |

## Evitar cobrança

Use instância free tier, **pare/termine** a instância depois da apresentação e não aloque Elastic IP sem associá-lo (IP parado é cobrado).
