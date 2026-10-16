#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

script_dir="$(dirname -- "${BASH_SOURCE[0]}")"
cd -- "$script_dir/.."
[[ -f "${BND_JAR:-$HOME/biz.aQute.bnd.jar}" ]] || { printf 'Missing bnd CLI\n' >&2; exit 2; }
./gradlew :io.klib.swtbot.support:jar :io.klib.swtbot.junit4:jar :io.klib.swtbot.jupiter:jar --no-daemon
java -jar "${BND_JAR:-$HOME/biz.aQute.bnd.jar}" repo -r 'SWTBot Test Support' put \
    io.klib.swtbot.support/generated/io.klib.swtbot.support.jar \
    io.klib.swtbot.junit4/generated/io.klib.swtbot.junit4.jar \
    io.klib.swtbot.jupiter/generated/io.klib.swtbot.jupiter.jar
java -jar "${BND_JAR:-$HOME/biz.aQute.bnd.jar}" repo list -f 'SWTBot Test Support'