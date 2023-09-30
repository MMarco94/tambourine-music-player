#!/usr/bin/env bash

set -ex

# TODO: remove ffmpeg libraries
# TODO: remove lwjdl
# TODO: remove jffi
# Build JAR
# TODO: use createReleaseDistributable
./gradlew createDistributable
tar -czf build/tmp/tambourine.tar.gz -C build/compose/binaries/main/app tambourine
# Create flatpak
flatpak-builder --force-clean build-dir flatpak/io.github.mmarco94.tambourine.yml
# Install flatpak
flatpak-builder --user --install --force-clean build-dir flatpak/io.github.mmarco94.tambourine.yml
