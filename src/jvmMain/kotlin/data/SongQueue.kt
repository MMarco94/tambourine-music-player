package data

data class SongQueue(
    val songs: List<Song>,
    val position: Int,
) {
    val currentSong get() = songs[position]

    init {
        require(position in songs.indices)
        require(songs.isNotEmpty())
    }

    fun previous(): SongQueue? {
        // TODO: repeat options go here
        return copy(position = (position - 1).mod(songs.size))
    }

    fun next(): SongQueue? {
        // TODO: repeat options go here
        return copy(position = (position + 1).mod(songs.size))
    }

    fun skipTo(song: Song): SongQueue {
        val iof = songs.indexOf(song)
        require(iof >= 0)
        return copy(position = iof)
    }
}