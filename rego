#!/bin/sh
cd `dirname $0`

thing=5
while [ "$thing" == "5" ]
do
    ./gradlew :repl:shadowJar || exit
    java -jar repl/build/libs/repl-all.jar
    thing="$?"
done

