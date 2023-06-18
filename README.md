# Tambourine Music Player

A music player for your local music library.

Tenets:

- Linux first
- Local only: no internet connection will ever be established.
- Read only: your music will be accessed only in read mode.
- Stateless: no cache/database/whatever will be created. The metadata in your songs _are_ the database.
- Imperfect: there will be use-cases that are not solved by this software, and that's fine.

## Features

TBD

## Roadmap

- Finish Flatpak packaging
- Search
- Metadata
  - Disk n°
  - Lyrics
- Usability
  - Playing a song, if the queue is identical, should just skip there
  - Option to add a song to current queue
  - Persist filter/sort
- Dynamically reload library if files change
- Drag&drop and open files outside the library
- Skip "zeros" at the beginning of the song
- Drag & drop in queue UI
- Spectrometer
- Background API?

