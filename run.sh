#!/usr/bin/env bash

mvn clean package && java -jar target/pan-cli-1.0-SNAPSHOT.jar -h 127.0.0.1 -p 6379