#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# docker image build -f Dockerfile-softhsm -t docker.eidastest.se:5000/eidas-connector-idp-hsm .

docker image build -f ${SCRIPT_DIR}/../../../Dockerfile-softhsm -t docker.eidastest.se:5000/eidas-connector-idp-hsm ${SCRIPT_DIR}/../../.. 

