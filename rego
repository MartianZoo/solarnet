#!/usr/bin/env bash

# https://stackoverflow.com/q/59895/113632
cd "$(dirname "${BASH_SOURCE[0]}")" || exit

readonly REBUILD_EXIT_CODE=5

# Rebuild in a loop as long as the process exits with the "rebuild" exit code
while true; do
    ./gradlew :repl:shadowJar || exit

    java -jar repl/build/libs/repl-all.jar
    exit_code=$?
    if (( exit_code != REBUILD_EXIT_CODE )); then
        exit "$exit_code"
    fi
done

