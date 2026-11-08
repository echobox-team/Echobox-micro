#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.local/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"
PROJECT_DIR="/Users/lucasbrisch/Documents/GitHub/Echobox-micro"
REGION="us-east-1"
CLUSTER_NAME="echobox-cluster"

mkdir -p "$HOME/.aws"

# Se o usuario copiou o bloco [default] do AWS Details (Ctrl+C / Cmd+C), salva automaticamente!
CLIPBOARD=$(pbpaste 2>/dev/null || true)
if echo "$CLIPBOARD" | grep -q "aws_access_key_id"; then
  echo ">>> Detectei as credenciais da AWS no seu Ctrl+C (Area de Transferencia)!"
  echo "$CLIPBOARD" > "$HOME/.aws/credentials"
  echo ">>> Credenciais salvas automaticamente em ~/.aws/credentials!"
fi

echo "============================================================"
echo " 1. Verificando conexao com a AWS Academy..."
echo "============================================================"
if ! aws sts get-caller-identity; then
  echo ""
  echo "ERRO: Suas credenciais da AWS expiraram ou nao foram copiadas!"
  echo "COMO RESOLVER:"
  echo "1. No site do AWS Academy, clique em 'AWS Details' -> 'Show' ao lado de AWS CLI."
  echo "2. Copie (Cmd+C) o bloco que comeca com [default]."
  echo "3. Rode novamente: religar-aws"
  exit 1
fi

echo "============================================================"
echo " 2. Verificando maquinas EC2 paradas pelo fim da sessao..."
echo "============================================================"
STOPPED_IDS=$(aws ec2 describe-instances \
  --region "$REGION" \
  --filters "Name=instance-state-name,Values=stopped" \
  --query "Reservations[].Instances[].InstanceId" \
  --output text)

if [ -n "$STOPPED_IDS" ]; then
  echo "Religando maquinas EC2 que estavam pausadas: $STOPPED_IDS"
  aws ec2 start-instances --region "$REGION" --instance-ids $STOPPED_IDS
  echo "Aguardando as maquinas EC2 ligarem (cerca de 1 minuto)..."
  aws ec2 wait instance-running --region "$REGION" --instance-ids $STOPPED_IDS
else
  echo "Maquinas EC2 ja estao ligadas!"
fi

echo "============================================================"
echo " 3. Conectando o kubectl ao cluster EKS ($CLUSTER_NAME)..."
echo "============================================================"
cd "$PROJECT_DIR"
if aws eks describe-cluster --region "$REGION" --name "$CLUSTER_NAME" >/dev/null 2>&1; then
  aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER_NAME"
  echo "Aguardando os Nodes do Kubernetes ficarem prontos..."
  kubectl wait --for=condition=Ready nodes --all --timeout=180s || true
  kubectl apply -f k8s/01-infrastructure.yaml
  kubectl apply -f k8s/02-microservices.yaml
  kubectl apply -f k8s/03-ingress.yaml
else
  echo "Cluster nao encontrado. Criando cluster..."
  ./scripts/create-eks-aws-academy.sh
  ./scripts/deploy-eks.sh
fi

echo "============================================================"
echo " 4. Status dos Pods e Links do Swagger UI:"
echo "============================================================"
kubectl get pods
LB_HOST=$(kubectl get ingress echobox-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo ""
echo "TUDO PRONTO! Abra estes 4 links no navegador:"
echo "1. ms-auth:         http://${LB_HOST}/ms-auth/swagger-ui/index.html"
echo "2. ms-company:      http://${LB_HOST}/ms-company/swagger-ui/index.html"
echo "3. ms-feedback:     http://${LB_HOST}/ms-feedback/swagger-ui.html"
echo "4. ms-notification: http://${LB_HOST}/ms-notification/swagger-ui/index.html"
