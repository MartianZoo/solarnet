#!/usr/bin/env bash

# https://stackoverflow.com/q/59895/113632
cd "$(dirname "${BASH_SOURCE[0]}")" || exit

readonly REBUILD_EXIT_CODE=5

if [[ -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 21)" || {
        echo "Error: JDK 21 is required. Install it or set JAVA_HOME to a JDK 21 install." >&2
        exit 1
    }
fi

if [[ ! -x "${JAVA_HOME:-}/bin/java" ]] || [[ "$("$JAVA_HOME/bin/java" -version 2>&1)" != *'version "21.'* ]]; then
    echo "Error: JDK 21 is required. Install it or set JAVA_HOME to a JDK 21 install." >&2
    exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"

# Rebuild in a loop as long as the process exits with the "rebuild" exit code
while true; do
    ./gradlew :interactive:shadowJar || exit

    "$JAVA_HOME/bin/java" -jar interactive/build/libs/interactive-all.jar
    exit_code=$?
    if (( exit_code != REBUILD_EXIT_CODE )); then
        exit "$exit_code"
    fi
done
