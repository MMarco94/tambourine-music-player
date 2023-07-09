# Tambourine Music Player

A music player for your local music library.

**THIS IS A PRE-RELEASE**, the project is still in development.

![Main screen](https://raw.githubusercontent.com/MMarco94/tambourine-music-player/main/screenshots/s1.png)

Tenets:

- Linux first
- Local only: no internet connection will ever be established.
- Read only: your music will be accessed only in read mode.
- Stateless: no cache/database/whatever will be created. The metadata in your songs _are_ the database.
- Imperfect: there will be use-cases that are not solved by this software, and that's fine.

## Features

Browse your music collection; play a song; that's it really.

## Screenshots

![s1](https://raw.githubusercontent.com/MMarco94/tambourine-music-player/main/screenshots/s1.png)
![s2](https://raw.githubusercontent.com/MMarco94/tambourine-music-player/main/screenshots/s2.png)
![s3](https://raw.githubusercontent.com/MMarco94/tambourine-music-player/main/screenshots/s3.png)
![s4](https://raw.githubusercontent.com/MMarco94/tambourine-music-player/main/screenshots/s4.png)
![s5](https://raw.githubusercontent.com/MMarco94/tambourine-music-player/main/screenshots/s6.png)
![s5](https://raw.githubusercontent.com/MMarco94/tambourine-music-player/main/screenshots/s5.png)

## Roadmap

Blockers for V1:
 - Use Flatpak file portal

Other non-blocking features:
- Metadata
  - Disk n°
  - Lyrics
- Translations
- Persist filter/sort
- Keep scrolling position when removing filters
- Drag&drop and open files to open them
- Skip "zeros" at the beginning of the song
- Drag & drop in queue UI
- Spectrometer
- Background API?
- Reduce `.jar` size
