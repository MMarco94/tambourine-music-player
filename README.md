# Music Player

A music player for your local music library.

Tenets:
 - Local only: no internet connection will ever be established.
 - Read only: your music will be accessed only in read mode.
 - Stateless: no cache/database/whatever will be created. The metadata in your songs _are_ the database.
 - Imperfect: there will be use-cases that are not solved by this software, and that's fine.

## Features

TBD

## Roadmap

- MPRIS(https://specifications.freedesktop.org/mpris-spec/latest/)
  - Implement remaining functions
- Search
- Metadata
  - Disk n°
  - Lyrics
- Usability
  - Playing a song, if the queue is identical, should just skip there
  - Scroll should be kept when changing tabs
  - Option to add a song to current queue
  - Persist filter/sort
- Dynamically reload library if files change
- Drag&drop and open files outside the library
- Skip "zeros" at the beginning of the song
- Improve theming based on current album image (https://github.com/SvenWoltmann/color-thief-java ?)
- UI for empty library
- Packaging
  - [ ] Flatpak
  - [ ] ...
- Drag & drop in queue UI
- Spectrometer
- [ ] Background API?


