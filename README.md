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

- Search
- Metadata
  - Disk n°
  - Lyrics
- Improve queue UI
  - Drag & drop
- Improve player UI
  - Improve spectrometer (should I sum instead of avg?)
- Improve theming based on current album image
- Improve library UI
  - Reduce vertical space of albums with few songs
- Usability
  - Playing a song, if the queue is identical, should just skip there
  - Scroll should be kept when changing tabs
  - Option to add a song to queue
  - Remember filter/sort
- Improve library load speed and memory consumption
- Packaging
  - [ ] Flatpak
  - [ ] ...
- Integration with the OS
  - [ ] Media notification
  - [ ] Media keys
  - [ ] Background API?
