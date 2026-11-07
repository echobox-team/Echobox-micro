#!/usr/bin/env bash
set -e

export PATH="/Applications/Docker.app/Contents/Resources/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

echo "============================================================"
echo "1. Instalando NGINX Ingress Controller no cluster EKS"
echo "============================================================"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/aws/deploy.yaml

echo "Aguardando o pod do NGINX Ingress Controller ficar pronto..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s || true

echo "============================================================"
echo "2. Implantando Bancos de Dados, RabbitMQ e MailHog (01-infrastructure.yaml)"
echo "============================================================"
kubectl apply -f k8s/01-infrastructure.yaml

echo "============================================================"
echo "3. Implantando os 4 Microsservicos (02-microservices.yaml)"
echo "============================================================"
kubectl apply -f k8s/02-microservices.yaml

echo "============================================================"
echo "4. Aplicando Regras de Roteamento no NGINX Ingress (03-ingress.yaml)"
echo "============================================================"
kubectl apply -f k8s/03-ingress.yaml

echo "============================================================"
echo "Status do Cluster Amazon EKS:"
echo "============================================================"
kubectl get pods -o wide
kubectl get services
kubectl get ingress

echo ""
echo "Endereco externo do LoadBalancer (NGINX Ingress):"
kubectl get svc -n ingress-nginx ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
echo ""
