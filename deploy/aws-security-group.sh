#!/usr/bin/env bash
set -Eeuo pipefail

# Cria (ou reaproveita) o Security Group da Sorveteria. Rode no AWS CloudShell
# (já vem com AWS CLI e credenciais da conta logada):
#   bash aws-security-group.sh <SEU_IP_PUBLICO>
# Ex.: bash aws-security-group.sh 177.10.20.30
# Libera somente: 22 (SSH) para o seu IP, 80 (HTTP) e 443 (HTTPS) públicos.
# A porta 5432 (PostgreSQL) NÃO é liberada: o banco só aceita conexões locais.

MY_IP=${1:-}
SG_NAME=${SG_NAME:-sorveteria-sg}
[[ $MY_IP =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || {
  echo "Uso: bash $0 <SEU_IP_PUBLICO>   (descubra em https://checkip.amazonaws.com)" >&2; exit 1; }

VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
[[ $VPC_ID != None ]] || { echo "Nenhuma VPC padrão encontrada nesta região." >&2; exit 1; }

SG_ID=$(aws ec2 describe-security-groups --filters Name=group-name,Values="$SG_NAME" Name=vpc-id,Values="$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text)
if [[ $SG_ID == None ]]; then
  SG_ID=$(aws ec2 create-security-group --group-name "$SG_NAME" --vpc-id "$VPC_ID" \
    --description "Sorveteria: SSH restrito, HTTP/HTTPS publicos, sem 5432" --query GroupId --output text)
  echo "Security Group criado: $SG_ID"
else
  echo "Security Group já existe: $SG_ID"
fi

allow() { # porta cidr descricao
  aws ec2 authorize-security-group-ingress --group-id "$SG_ID" \
    --ip-permissions "IpProtocol=tcp,FromPort=$1,ToPort=$1,IpRanges=[{CidrIp=$2,Description=$3}]" \
    >/dev/null 2>&1 && echo "  liberado $1/tcp para $2" || echo "  $1/tcp para $2 já estava liberado"
}
allow 22  "$MY_IP/32" "SSH-admin"
allow 80  "0.0.0.0/0" "HTTP"
allow 443 "0.0.0.0/0" "HTTPS"

echo
aws ec2 describe-security-groups --group-ids "$SG_ID" \
  --query 'SecurityGroups[0].IpPermissions[].{Porta:FromPort,Origem:IpRanges[0].CidrIp}' --output table
echo "Use o Security Group $SG_NAME ($SG_ID) ao lançar a instância EC2."
