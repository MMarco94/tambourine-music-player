# Development

## Running

```
./gradlew run
```

## Packaging

Tambourine uses Flatpak as the main packaging system.

Make sure you have the following installed:

- `flatpak-builder`
- Flatpak runtimes:
  ```
  flatpak install flathub org.freedesktop.Platform//24.08 org.freedesktop.Sdk//24.08 org.freedesktop.Platform.ffmpeg-full//24.08
  ```

To build and install:

```
./flatpak/build.sh
```

To run:

```
flatpak run io.github.mmarco94.tambourine
```

