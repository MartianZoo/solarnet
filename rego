#!/usr/bin/env bash

# https://stackoverflow.com/q/59895/113632
cd "$(dirname "${BASH_SOURCE[0]}")" || exit

readonly REBUILD_EXIT_CODE=5

if [[ -x "${JAVA_HOME:-}/bin/java" ]]; then
    readonly JAVA_CMD="$JAVA_HOME/bin/java"
else
    readonly JAVA_CMD=java
fi

# Rebuild in a loop as long as the process exits with the "rebuild" exit code
while true; do
    jar_path=$(./gradlew --quiet :repl:shadowJarPath | tail -n 1)
    if [[ ! -f "$jar_path" ]]; then
        printf 'Error: could not build the REPL jar.\n' >&2
        exit 1
    fi

    "$JAVA_CMD" -jar "$jar_path"
    exit_code=$?
    if (( exit_code != REBUILD_EXIT_CODE )); then
        exit "$exit_code"
    fi
done
