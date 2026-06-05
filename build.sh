#!/bin/bash
echo "Docker build script"
# Build script
set -eo pipefail
build_tag=$1
name=sb-hub-graph-service
node=$2
org=$3

# Build the jar using Dockerfile.build, then extract it to context root
docker build -f ./Dockerfile.build -t ${name}-build:${build_tag} .
id=$(docker create ${name}-build:${build_tag})
docker cp $id:/opt/target/hub-services-0.0.1-SNAPSHOT.jar ./hub-services-0.0.1-SNAPSHOT.jar
docker rm -v $id

docker build -f ./Dockerfile --label commitHash=$(git rev-parse --short HEAD) -t ${org}/${name}:${build_tag} .
echo {\"image_name\" : \"${name}\", \"image_tag\" : \"${build_tag}\", \"node_name\" : \"$node\"} > metadata.json
