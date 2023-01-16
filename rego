#!/bin/sh
cd `dirname $0`
./gradlew :repl:shadowJar && java -jar repl/build/libs/repl-all.jar

