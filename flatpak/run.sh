#!/usr/bin/env bash

set -ex

 # Printing the local time, as a reference to measure startup performances
date +"%T.%N"

# Setting up tmp dir
rm -f "$XDG_CACHE_HOME"/tmp/*.png
mkdir -p "$XDG_CACHE_HOME"/tmp

export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$XDG_CACHE_HOME/tmp -Djava.util.prefs.userRoot=$XDG_CONFIG_HOME"
#export MALLOC_ARENA_MAX=8
export MALLOC_TRIM_THRESHOLD_=131072 # 128k
# Printing the JAR's stat for debugging
stat /app/tambourine/lib/app/tambourine-*.jar

LD_LIBRARY_PATH=/app/tambourine/lib/os exec /app/tambourine/bin/tambourine "$@"