#!/bin/bash
#
#
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export DOCKER_REPO=ghcr.io

if [ -z "$GITHUB_USER" ]; then
  echo "The GITHUB_USER variable must be set"
  exit 1
fi

if [ -z "$GITHUB_ACCESS_TOKEN" ]; then
  echo "The GITHUB_ACCESS_TOKEN variable must be set"
  exit 1
fi

echo "Logging in to ${DOCKER_REPO} ..."
echo $GITHUB_ACCESS_TOKEN | docker login $DOCKER_REPO -u $GITHUB_USER --password-stdin

mvn -f ${SCRIPT_DIR}/../idp/pom.xml clean install jib:build    

