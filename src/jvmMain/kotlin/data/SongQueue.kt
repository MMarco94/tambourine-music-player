package data

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SongQueue {
    private var index by mutableStateOf(0)
    private var _songs = mutableStateOf(emptyList<Song>())
    val songs by _songs
    val currentSong by derivedStateOf {
        songs.getOrNull(index)
    }

    fun setSongs(songs: List<Song>, current: Song) {
        val iof = songs.indexOf(current)
        require(iof >= 0)
        index = iof
        _songs.value = songs
    }

    fun popNext(): Song? {
        return songs.getOrNull(++index)
    }

    fun skipTo(song: Song) {
        val iof = songs.indexOf(song)
        if (iof != -1) {
            index = iof
        }
    }
}
