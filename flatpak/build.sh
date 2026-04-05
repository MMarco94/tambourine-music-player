#!/usr/bin/env bash

set -ex

# Build JAR
# Uncomment for release builds
#./gradlew createReleaseDistributable
./gradlew createReleaseDistributable -POptimizeProGuard=false

# Running the app to collect classlist
cp build/compose/binaries/main-release/app/tambourine/lib/app/tambourine.cfg build/compose/binaries/main-release/app/tambourine/lib/app/tambourine.cfg.bkp
echo "java-options=-Xshare:off" >> build/compose/binaries/main-release/app/tambourine/lib/app/tambourine.cfg
echo "java-options=-XX:DumpLoadedClassList=build/compose/binaries/main-release/app/tambourine/lib/app/resources/tambourine.classlist" >> build/compose/binaries/main-release/app/tambourine/lib/app/tambourine.cfg
build/compose/binaries/main-release/app/tambourine/bin/tambourine

mv build/compose/binaries/main-release/app/tambourine/lib/app/tambourine.cfg.bkp build/compose/binaries/main-release/app/tambourine/lib/app/tambourine.cfg


# Build and install flatpak
flatpak-builder --user --install --force-clean build-dir flatpak/io.github.mmarco94.tambourine.yml
# Clean install
rm -rf "$HOME"/.var/app/io.github.mmarco94.tambourine