#!/usr/bin/env bash

set -ex

# Build JAR
./gradlew createReleaseDistributable
tar -czf build/tmp/tambourine.tar.gz -C build/compose/binaries/main-release/app tambourine
# Create flatpak
flatpak-builder --force-clean build-dir flatpak/io.github.mmarco94.tambourine.yml
# Install flatpak
flatpak-builder --user --install --force-clean build-dir flatpak/io.github.mmarco94.tambourine.yml
# Clean install
rm -rf "$HOME"/.var/app/io.github.mmarco94.tambourine