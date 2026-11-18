#!/usr/bin/env bash
# Select this branch's Vaadin release without changing the Vidocq submodule.
set -euo pipefail
cd "$(dirname "$0")/03-vidocq"
exec mvn -Dflow.version=25.4.0-beta1 "$@"
