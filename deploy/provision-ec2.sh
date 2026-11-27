#!/usr/bin/env bash
set -Eeuo pipefail

# Uso em uma EC2 Ubuntu 22.04/24.04 (recomendado 24.04, t2.micro/t3.micro):
#   Opção A (sem clonar antes):
#     curl -fsSL https://raw.githubusercontent.com/MiguelSteinmetz/sorveteria/master/deploy/provision-ec2.sh -o provision.sh
#     sudo bash provision.sh
#   Opção B (dentro do clone):
#     sudo bash deploy/provision-ec2.sh
#   Sem perguntas (opcional):
#     sudo ADMIN_LOGIN=gerente ADMIN_PASSWORD='SenhaForte123!' bash provision.sh
# Security Group (AWS Console ou deploy/aws-security-group.sh no CloudShell):
# SSH (22) restrito ao seu IP, HTTP (80) e HTTPS (443) públicos; NUNCA 5432.

APP_NAME=sorveteria
APP_USER=sorveteria
APP_HOME=/opt/sorveteria
RELEASES="$APP_HOME/releases"
CURRENT="$APP_HOME/current"
ENV_DIR=/etc/sorveteria
ENV_FILE="$ENV_DIR/sorveteria.env"
REPO_URL=${REPO_URL:-https://github.com/MiguelSteinmetz/sorveteria.git}
REPO_BRANCH=${REPO_BRANCH:-master}
SRC_DIR="$APP_HOME/src"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

fail() { echo "ERRO: $*" >&2; exit 1; }
step() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
[[ $EUID -eq 0 ]] || fail "execute com sudo: sudo bash $0"
. /etc/os-release
[[ ${ID:-} == ubuntu && ( ${VERSION_ID:-} == 22.04 || ${VERSION_ID:-} == 24.04 ) ]] \
  || fail "suportado somente Ubuntu 22.04/24.04 (detectado ${PRETTY_NAME:-desconhecido})"

# Se o script foi baixado sozinho (fora do clone), clona/atualiza o repositório.
if [[ ! -f "$REPO_ROOT/pom.xml" ]]; then
  step "Baixando código-fonte ($REPO_URL, branch $REPO_BRANCH)"
  command -v git >/dev/null || { apt-get update -qq && apt-get install -y -qq git; }
  if [[ -d "$SRC_DIR/.git" ]]; then
    git -C "$SRC_DIR" fetch --depth 1 origin "$REPO_BRANCH"
    git -C "$SRC_DIR" reset --hard FETCH_HEAD
  else
    install -d -m 0755 "$APP_HOME"
    git clone --depth 1 --branch "$REPO_BRANCH" "$REPO_URL" "$SRC_DIR"
  fi
  REPO_ROOT="$SRC_DIR"
fi
[[ -f "$REPO_ROOT/pom.xml" && -f "$REPO_ROOT/package.json" ]] \
  || fail "execute a partir de um clone contendo pom.xml e package.json"
[[ -f "$REPO_ROOT/src/app/app.config.ts" && -f "$REPO_ROOT/src/app/app.component.ts" ]] \
  || fail "frontend Angular incompleto: faltam src/app/app.config.ts e/ou app.component.ts"

if ! id -u "$APP_USER" >/dev/null 2>&1; then useradd --system --home-dir "$APP_HOME" --shell /usr/sbin/nologin "$APP_USER"; fi

if [[ ! -f "$ENV_FILE" ]]; then
  step "Criando configuração protegida em $ENV_FILE (senha do banco e JWT_SECRET gerados aleatoriamente)"
  DB_NAME=${DB_NAME:-sorveteria}
  DB_USER=${DB_USER:-sorveteria_app}
  [[ $DB_NAME =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ && $DB_USER =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] || fail "nome de banco/usuário inválido"
  DB_PASSWORD=$(openssl rand -hex 24)
  JWT_SECRET=$(openssl rand -hex 48)
  [[ -n ${ADMIN_LOGIN:-} ]] || read -r -p "Login inicial do gerente [gerente]: " ADMIN_LOGIN
  ADMIN_LOGIN=${ADMIN_LOGIN:-gerente}
  [[ -n ${ADMIN_PASSWORD:-} ]] || { read -r -s -p "Senha inicial do gerente (mín. 12 caracteres): " ADMIN_PASSWORD; printf '\n'; }
  [[ ${#ADMIN_PASSWORD} -ge 12 ]] || fail "a senha do gerente deve ter pelo menos 12 caracteres"
  install -d -o root -g "$APP_USER" -m 0750 "$ENV_DIR"
  umask 077
  cat > "$ENV_FILE" <<EOF
DB_URL=jdbc:postgresql://localhost:5432/${DB_NAME}
DB_USERNAME=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=2h
CORS_ALLOWED_ORIGINS=http://localhost
ADMIN_LOGIN=${ADMIN_LOGIN}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
EOF
  chown root:"$APP_USER" "$ENV_FILE"
  chmod 0640 "$ENV_FILE"
  unset DB_PASSWORD JWT_SECRET ADMIN_PASSWORD
fi

# Free tier (1 GB RAM) não aguenta Maven + Angular + Java juntos sem swap.
if ! swapon --show | grep -q .; then
  step "Criando swap de 2 GB (evita falta de memória no build)"
  fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

step "Instalando dependências (Nginx, PostgreSQL, Java 17, Maven, Node 20)"
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg nginx postgresql postgresql-contrib openjdk-17-jdk maven git ufw
# Angular 19 requer Node.js >=18.19; instale Node 20 LTS pelo repositório oficial NodeSource.
install -d -m 0755 /etc/apt/keyrings
curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor --yes -o /etc/apt/keyrings/nodesource.gpg
chmod 0644 /etc/apt/keyrings/nodesource.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" > /etc/apt/sources.list.d/nodesource.list
apt-get update
apt-get install -y nodejs

install -d -o "$APP_USER" -g "$APP_USER" -m 0750 "$APP_HOME" "$RELEASES" "$APP_HOME/frontend"
install -d -o root -g root -m 0755 /etc/systemd/system /etc/nginx/sites-available /etc/nginx/sites-enabled

step "Configurando PostgreSQL (somente localhost)"
systemctl enable --now postgresql
# PostgreSQL só escuta loopback; a porta 5432 não deve ser publicada no Security Group.
sed -ri "s/^[# ]*listen_addresses[[:space:]]*=.*/listen_addresses = 'localhost'/" /etc/postgresql/*/main/postgresql.conf
systemctl restart postgresql
# Carrega variáveis do arquivo protegido sem expor a senha em argumentos de processo.
set -a; # shellcheck disable=SC1090
. "$ENV_FILE"
set +a
DB_NAME=${DB_URL##*/}; DB_NAME=${DB_NAME%%\?*}
DB_USER=${DB_USERNAME}
[[ $DB_NAME =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ && $DB_USER =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] || fail "DB_URL/DB_USERNAME inválidos no $ENV_FILE"
export DB_PASSWORD_HEX="$(printf '%s' "$DB_PASSWORD" | od -An -tx1 | tr -d ' \n')"
sudo -u postgres psql -v ON_ERROR_STOP=1 -v db_name="$DB_NAME" -v db_user="$DB_USER" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN', :'db_user')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'db_user')\gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'db_user', convert_from(decode(:'db_password_hex', 'hex'), 'UTF8'))\gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'db_name', :'db_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db_name')\gexec
SQL
unset DB_PASSWORD_HEX

# Builda backend e frontend. Interrupção se qualquer build falhar.
cd "$REPO_ROOT"
step "Build do back-end (Spring Boot)"
MAVEN_OPTS="-Xmx512m" mvn -B -q -DskipTests package
step "Build de produção do front-end (Angular)"
export NODE_OPTIONS=--max-old-space-size=1536
npm ci --no-audit --no-fund
npm run build -- --configuration production
JAR=$(find "$REPO_ROOT/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)
[[ -n $JAR ]] || fail "JAR do Spring Boot não encontrado em target/"
FRONTEND_INDEX=$(find "$REPO_ROOT/dist" -type f -path '*/browser/index.html' -print -quit)
[[ -n $FRONTEND_INDEX ]] || FRONTEND_INDEX=$(find "$REPO_ROOT/dist" -type f -name index.html -print -quit)
[[ -n $FRONTEND_INDEX ]] || fail "build Angular não gerou index.html em dist/"
FRONTEND_DIR=$(dirname "$FRONTEND_INDEX")
RELEASE="$RELEASES/$(date -u +%Y%m%d%H%M%S)"
install -d -o "$APP_USER" -g "$APP_USER" -m 0750 "$RELEASE" "$RELEASE/frontend"
install -o "$APP_USER" -g "$APP_USER" -m 0640 "$JAR" "$RELEASE/app.jar"
cp -a "$FRONTEND_DIR/." "$RELEASE/frontend/"
chown -R "$APP_USER:$APP_USER" "$RELEASE"
ln -sfn "$RELEASE" "$CURRENT.new"
mv -Tf "$CURRENT.new" "$CURRENT"

step "Publicando release, systemd e Nginx"
install -m 0644 "$REPO_ROOT/deploy/sorveteria.service" /etc/systemd/system/sorveteria.service
sed "s|@@APP_HOME@@|$APP_HOME|g" "$REPO_ROOT/deploy/nginx-sorveteria.conf" > /etc/nginx/sites-available/sorveteria
ln -sfn /etc/nginx/sites-available/sorveteria /etc/nginx/sites-enabled/sorveteria
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl daemon-reload
systemctl enable --now sorveteria
systemctl restart sorveteria
systemctl enable nginx
systemctl reload nginx

# Firewall host; mantenha regras do Security Group coerentes com este conjunto.
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

step "Verificando a aplicação"
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/api/produtos || true)
  [[ $code == 401 || $code == 403 || $code == 200 ]] && break
  sleep 3
done
systemctl is-active --quiet sorveteria || { journalctl -u sorveteria -n 50 --no-pager; fail "serviço sorveteria não subiu"; }
echo "API sem token -> HTTP $code (esperado 401: rota protegida por JWT)"
echo "Front (rota SPA /login) -> HTTP $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/login)"
echo "PostgreSQL escutando em: $(ss -ltnH 'sport = :5432' | awk '{print $4}' | xargs)"

TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' || true)
PUBLIC_IP=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4 || true)
echo
echo "Deploy concluído!  Acesse: http://${PUBLIC_IP:-<IP-publico-da-EC2>}/"
echo "Logs: journalctl -u sorveteria -f   |   Status: systemctl status sorveteria"
echo "Security Group: 22 restrita ao seu IP; 80/443 públicos; NÃO liberar 5432."
