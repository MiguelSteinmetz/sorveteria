#!/usr/bin/env bash
set -Eeuo pipefail

# Execute na raiz do clone em uma EC2 Ubuntu 22.04/24.04:
#   sudo bash deploy/provision-ec2.sh
# O Security Group deve ser preparado separadamente no AWS Console/Terraform:
# SSH (22) restrito ao IP administrativo, HTTP (80) e HTTPS (443); nunca 5432.

APP_NAME=sorveteria
APP_USER=sorveteria
APP_HOME=/opt/sorveteria
RELEASES="$APP_HOME/releases"
CURRENT="$APP_HOME/current"
ENV_DIR=/etc/sorveteria
ENV_FILE="$ENV_DIR/sorveteria.env"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

fail() { echo "ERRO: $*" >&2; exit 1; }
[[ $EUID -eq 0 ]] || fail "execute com sudo: sudo bash deploy/provision-ec2.sh"
. /etc/os-release
[[ ${ID:-} == ubuntu && ( ${VERSION_ID:-} == 22.04 || ${VERSION_ID:-} == 24.04 ) ]] \
  || fail "suportado somente Ubuntu 22.04/24.04 (detectado ${PRETTY_NAME:-desconhecido})"
[[ -f "$REPO_ROOT/pom.xml" && -f "$REPO_ROOT/package.json" ]] \
  || fail "execute a partir de um clone contendo pom.xml e package.json"
[[ -f "$REPO_ROOT/src/app/app.config.ts" && -f "$REPO_ROOT/src/app/app.component.ts" ]] \
  || fail "frontend Angular incompleto: faltam src/app/app.config.ts e/ou app.component.ts"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Criando configuração inicial protegida em $ENV_FILE"
  read -r -p "Nome do banco [sorveteria]: " DB_NAME; DB_NAME=${DB_NAME:-sorveteria}
  read -r -p "Usuário PostgreSQL [sorveteria_app]: " DB_USER; DB_USER=${DB_USER:-sorveteria_app}
  [[ $DB_NAME =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ && $DB_USER =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] || fail "nome de banco/usuário inválido"
  DB_PASSWORD=$(openssl rand -hex 24)
  JWT_SECRET=$(openssl rand -hex 48)
  read -r -p "Login inicial do gerente: " ADMIN_LOGIN
  [[ -n $ADMIN_LOGIN ]] || fail "login do gerente não pode ser vazio"
  read -r -s -p "Senha inicial do gerente: " ADMIN_PASSWORD; printf '\n'
  [[ ${#ADMIN_PASSWORD} -ge 12 ]] || fail "a senha do gerente deve ter pelo menos 12 caracteres"
  install -d -o root -g "$APP_USER" -m 0750 "$ENV_DIR" 2>/dev/null || install -d -o root -g root -m 0750 "$ENV_DIR"
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
  chown root:"$APP_USER" "$ENV_FILE" 2>/dev/null || chown root:root "$ENV_FILE"
  chmod 0640 "$ENV_FILE"
  unset DB_PASSWORD JWT_SECRET ADMIN_PASSWORD
fi

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

if ! id -u "$APP_USER" >/dev/null 2>&1; then useradd --system --home-dir "$APP_HOME" --shell /usr/sbin/nologin "$APP_USER"; fi
install -d -o "$APP_USER" -g "$APP_USER" -m 0750 "$APP_HOME" "$RELEASES" "$APP_HOME/frontend"
install -d -o root -g root -m 0755 /etc/systemd/system /etc/nginx/sites-available /etc/nginx/sites-enabled

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
mvn -B -DskipTests package
npm ci
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

echo "Deploy concluído. Consulte: systemctl status sorveteria; journalctl -u sorveteria -f"
echo "Acesse http://<IP-ou-DNS-publico-da-EC2>/"
echo "Security Group: 22 restrita ao IP administrativo; 80/443 públicos; não liberar 5432."
