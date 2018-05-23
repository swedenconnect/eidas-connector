#!/bin/bash

docker run --name shib-redis -d -p 6379:6379 redis

docker logs -f shib-redis