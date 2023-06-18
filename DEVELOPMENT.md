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
  flatpak install flathub runtime/org.freedesktop.Sdk.Extension.openjdk17//22.08 
  ```

To build and package:

```
./flatpak/build.sh
```

To install:

```
flatpak run io.github.mmarco94.tambourine
```

## TODO

- Do not access the whole DBUS
- Access the FS through the file portal; see https://github.com/ATLauncher/ATLauncher/issues/564
