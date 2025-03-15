#!/usr/bin/env bash

set -ex

# Setting up tmp dir
rm -f "$XDG_CACHE_HOME"/tmp/*.png
mkdir -p "$XDG_CACHE_HOME"/tmp

export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$XDG_CACHE_HOME/tmp -Djava.util.prefs.userRoot=$XDG_CONFIG_HOME"

LD_LIBRARY_PATH=/app/tambourine/lib/os exec /app/tambourine/bin/tambourine "$@"