#!/usr/bin/env bash
set -e

export PATH="$HOME/.local/bin:/Applications/Docker.app/Contents/Resources/bin:/opt/homebrew/bin:$PATH"

CLUSTER_NAME="echobox-cluster"
NODEGROUP_NAME="echobox-nodes"
REGION="us-east-1"

echo "1. Obtendo LabRole ARN da conta AWS Academy..."
LAB_ROLE_ARN=$(aws iam get-role --role-name LabRole --query 'Role.Arn' --output text)
echo "LabRole: $LAB_ROLE_ARN"

echo "2. Obtendo Subnets das zonas us-east-1a, us-east-1b e us-east-1c..."
SUBNETS=$(aws ec2 describe-subnets \
  --filters "Name=availability-zone,Values=us-east-1a,us-east-1b,us-east-1c" \
  --query "Subnets[*].SubnetId" --output text | tr '\t' ',')
SUBNET_LIST=$(echo "$SUBNETS" | tr ',' ' ')
echo "Subnets: $SUBNETS"

echo "3. Adicionando tags de LoadBalancer nas Subnets para o NGINX Ingress..."
aws ec2 create-tags --resources $SUBNET_LIST \
  --tags "Key=kubernetes.io/cluster/$CLUSTER_NAME,Value=shared" "Key=kubernetes.io/role/elb,Value=1"

echo "4. Criando Cluster Amazon EKS ($CLUSTER_NAME)..."
if ! aws eks describe-cluster --name "$CLUSTER_NAME" >/dev/null 2>&1; then
  aws eks create-cluster \
    --name "$CLUSTER_NAME" \
    --region "$REGION" \
    --role-arn "$LAB_ROLE_ARN" \
    --resources-vpc-config "subnetIds=$SUBNETS" >/dev/null
fi

echo "5. Aguardando o Cluster EKS ficar ACTIVE (leva ~8 a 10 minutos na AWS)..."
aws eks wait cluster-active --name "$CLUSTER_NAME" --region "$REGION"
echo "Cluster $CLUSTER_NAME esta ACTIVE!"

echo "6. Configurando kubectl (update-kubeconfig)..."
aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER_NAME"

echo "7. Criando NodeGroup EC2 ($NODEGROUP_NAME com 2 instancias t3.medium)..."
if ! aws eks describe-nodegroup --cluster-name "$CLUSTER_NAME" --nodegroup-name "$NODEGROUP_NAME" >/dev/null 2>&1; then
  aws eks create-nodegroup \
    --cluster-name "$CLUSTER_NAME" \
    --nodegroup-name "$NODEGROUP_NAME" \
    --node-role "$LAB_ROLE_ARN" \
    --subnets $SUBNET_LIST \
    --instance-types t3.medium \
    --scaling-config minSize=2,maxSize=3,desiredSize=2 >/dev/null
fi

echo "8. Aguardando os Worker Nodes EC2 ficarem ACTIVE (leva ~3 a 5 minutos)..."
aws eks wait nodegroup-active --cluster-name "$CLUSTER_NAME" --nodegroup-name "$NODEGROUP_NAME" --region "$REGION"
echo "NodeGroup $NODEGROUP_NAME esta ACTIVE!"

echo "9. Implantando NGINX Ingress Controller e Microsservicos Echobox..."
./scripts/deploy-eks.sh
