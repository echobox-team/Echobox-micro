#!/usr/bin/env bash
set -e

export PATH="/Applications/Docker.app/Contents/Resources/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

DOCKERHUB_USER="${1:-lucaszanlorenzi}"
VERSION="1.0.0"

echo "============================================================"
echo "1. Construindo e enviando imagens para $DOCKERHUB_USER (linux/amd64)"
echo "============================================================"

for SERVICE in ms-auth ms-company ms-feedback ms-notification; do
  echo "-> Building $DOCKERHUB_USER/$SERVICE:$VERSION ..."
  docker build --platform linux/amd64 -t "$DOCKERHUB_USER/$SERVICE:$VERSION" "./$SERVICE"
  echo "-> Pushing $DOCKERHUB_USER/$SERVICE:$VERSION to Docker Hub..."
  docker push "$DOCKERHUB_USER/$SERVICE:$VERSION"
done

echo "============================================================"
echo "2. Garantindo usuario $DOCKERHUB_USER em k8s/02-microservices.yaml"
echo "============================================================"
sed -i.bak "s|DOCKERHUB_USER|$DOCKERHUB_USER|g" k8s/02-microservices.yaml
rm -f k8s/02-microservices.yaml.bak

echo "Concluido! As 4 imagens estao publicadas em https://hub.docker.com/u/$DOCKERHUB_USER"
