#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

script_dir="$(dirname -- "${BASH_SOURCE[0]}")"
cd -- "$script_dir/.."
bnd_jar="${BND_JAR:-$HOME/biz.aQute.bnd.jar}"
for required in java mktemp; do
    command -v "$required" >/dev/null || { printf 'required: %s\n' "$required" >&2; exit 2; }
done
[[ -f "$bnd_jar" ]] || { printf 'Missing bnd CLI: %s\n' "$bnd_jar" >&2; exit 2; }
./gradlew :io.klib.swtbot.support:test :io.klib.swtbot.junit4:test \
    :io.klib.swtbot.jupiter:test :io.klib.swtbot.support.tests:jar --no-daemon
mkdir -p reports
report_dir="$(mktemp -d "$PWD/reports/verify.XXXXXXXX")"
for runtime in junit4 jupiter5 jupiter6; do
    launch="io.klib.swtbot.support.tests/$runtime.win32.bndrun"
    java -jar "$bnd_jar" resolve resolve -W "$launch"
    java -jar "$bnd_jar" runtests -r "$report_dir/$runtime" "$launch"
done
java scripts/VerifyReports.java "$report_dir"