#!/bin/bash

NAME=$1
VERSION=$2
SUPPORT_IMAGE=java-with-openvpn:0.1

if [[ "$(docker images -q $SUPPORT_IMAGE 2> /dev/null)" == "" ]]; then
  echo "#### Building intermediate image..."
  docker build -t $SUPPORT_IMAGE -f ./support-image.docker ./
  echo "#### OK"
fi

echo "#### Remove old containers and images..."
docker rm --force "$NAME"
docker rmi "$NAME:$VERSION"
echo "#### OK"
echo "#### Building new image and run a container..."
docker build -t "$NAME:$VERSION" -f ./server-ui.docker ./ && \
  docker run --cap-add NET_ADMIN --name "$NAME" --net host -v /docker/data/openvpn:/openvpn/data -d "$NAME:$VERSION"
echo "#### OK"
exit 0
