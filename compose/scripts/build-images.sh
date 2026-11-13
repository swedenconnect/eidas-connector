#!/usr/bin/env bash
#
# Copyright 2017-2026 Sweden Connect
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Builds the Docker images of the eIDAS connector and the eIDAS simulator from this repository, into the local
# Docker daemon, as "eidas-connector" and "eidas-simulator". These are the images that docker-compose.yml uses.
#
# The modules are installed in the local Maven repository first, since each image is built from its own module.
# Any arguments are passed on to Maven, for example -o to build offline.
#
# Usage: compose/scripts/build-images.sh [maven arguments]
#
# Requires Java 21, Maven and a running Docker daemon.
#

set -euo pipefail

REPO="$(cd "$(dirname "$0")/../.." && pwd)"

cd "${REPO}"
echo "Building and installing the modules ..."
mvn -B -q -DskipTests "$@" clean install
echo "Building the image eidas-connector ..."
mvn -B -q "$@" -f idp/pom.xml jib:dockerBuild@local
echo "Building the image eidas-simulator ..."
mvn -B -q "$@" -f eidas-simulator/pom.xml jib:dockerBuild@local
echo "Done."
