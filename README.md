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

 - Support for multiple music formats
   - [ ] FLAC
   - [ ] ...
 - Search
 - Lyrics
 - Improve queue UI
 - Improve library load speed and memory consumption
 - Packaging
   - [ ] Flatpak
   - [ ] ...
 - Integration with the OS
   - [ ] Media controls
   - [ ] Background API?
 - Keyboard shortcuts (e.g. space to play/pause)
 - Seamless track transition