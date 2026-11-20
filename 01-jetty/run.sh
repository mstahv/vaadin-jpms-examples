#!/usr/bin/env bash
# Builds the application and starts it from the module path.
set -euo pipefail
cd "$(dirname "$0")"
# Clean removes old dependency jars when switching versions or Maven profiles.
mvn -B -q -nsu clean package "$@"
exec java -p target/modules:target/jpms-example-jetty-1.0-SNAPSHOT.jar \
    -m com.example.jpms.jetty/com.example.jpms.jetty.Main
