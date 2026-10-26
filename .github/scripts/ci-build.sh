#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

bash ./gradlew --no-daemon --version
bash ./gradlew --no-daemon --console=plain --continue build "$@"
