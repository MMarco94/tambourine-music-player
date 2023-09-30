# Development

## Running

```
./gradlew run
```

## Packaging

Tambourine uses Flatpak as the main packaging system.

Make sure you have the following installed:

- `flatpak-builder`
- Flatpack runtimes:
  ```
  flatpak install flathub org.freedesktop.Platform//22.08 org.freedesktop.Sdk//22.08 
  flatpak install flathub org.freedesktop.Platform.ffmpeg-full//22.08
  ```

To build and install:

```
./flatpak/build.sh
```

To run:

```
flatpak run io.github.mmarco94.tambourine
```

