#!/usr/bin/env bash

set -ex

# TODO: remove ffmpeg libraries
# TODO: remove lwjdl
# TODO: remove jffi
# Build JAR
./gradlew packageReleaseUberJarForCurrentOS
# Create flatpak
flatpak-builder --repo=repo --force-clean build-dir flatpak/io.github.mmarco94.tambourine.yml
# Update Flatpak with the local repository
flatpak build-update-repo repo
# Install flatpak
flatpak-builder --user --install --force-clean build-dir flatpak/io.github.mmarco94.tambourine.yml

# TODO: remove once issue is fixed
# Clearing the `.skiko` folder, to test the workaround for https://github.com/JetBrains/skiko/issues/731
rm -r ~/.skiko