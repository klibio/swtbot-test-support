#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

./gradlew --no-daemon --version
./gradlew --no-daemon --console=plain --continue build "$@"
