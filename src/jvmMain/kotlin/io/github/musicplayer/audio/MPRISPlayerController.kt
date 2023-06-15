package io.github.musicplayer.audio

import io.github.musicplayer.data.RepeatMode.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant
import org.mpris.MediaPlayer2.LoopStatus
import org.mpris.MediaPlayer2.LoopStatus.None
import org.mpris.MediaPlayer2.LoopStatus.Playlist
import org.mpris.MediaPlayer2.PlaybackStatus
import xerus.mpris.*
import kotlin.time.Duration.Companion.microseconds

class MPRISPlayerController(
    private val cs: CoroutineScope,
    private val playerController: PlayerController,
) : AbstractMPRISPlayer(), MediaPlayerX, PlayerX, Properties {

    fun setState(state: PlayerController.State) {
        val currentSong = state.currentlyPlaying?.queue?.currentSong

        canControl = state.currentlyPlaying != null
        canGoNext = state.currentlyPlaying != null
        canGoPrevious = state.currentlyPlaying != null
        canPlay = state.currentlyPlaying != null
        canPause = state.currentlyPlaying != null
        canSeek = state.currentlyPlaying != null

        playbackStatus = if (state.currentlyPlaying == null) {
            PlaybackStatus.Stopped
        } else if (state.pause) {
            PlaybackStatus.Paused
        } else {
            PlaybackStatus.Playing
        }

        loopStatus = if (state.currentlyPlaying == null) {
            None
        } else when (state.currentlyPlaying.queue.repeatMode) {
            DO_NOT_REPEAT -> None
            REPEAT_QUEUE -> Playlist
            REPEAT_SONG -> LoopStatus.Track
        }

        shuffle = state.currentlyPlaying != null && state.currentlyPlaying.queue.isShuffled
        position = state.position.inWholeMicroseconds
        metadata = if (currentSong == null) emptyMap() else HashMap(PropertyMap {
            // See https://www.freedesktop.org/wiki/Specifications/mpris-spec/metadata
            // TODO: this supports lyrics!
            // TODO: a better ID
            put("mpris:trackid", currentSong.hashCode().toString())
            put("mpris:length", currentSong.length.inWholeMicroseconds)
            //TODO: put("mpris:artUrl", cover)
            put("xesam:artist", currentSong.artist.name)
            put("xesam:album", currentSong.album.title)
            put("xesam:title", currentSong.title)
            if (currentSong.track != null) {
                put("xesam:discNumber", currentSong.track)
            }
        })
    }

    // TODO: is this correct?
    override val supportedUriSchemes: Array<String> by DBusConstant(arrayOf("file"))
    override val supportedMimeTypes: Array<String> by DBusConstant(arrayOf("audio/mpeg", "audio/mp4"))

    override val canRaise by DBusConstant(false)
    override val canQuit by DBusConstant(true)
    override val canSetFullscreen by DBusConstant(false)
    override var fullscreen: Boolean
        get() = false
        set(value) {}
    override val identity: String by DBusConstant("Music Player")
    override val desktopEntry: String by DBusConstant("TODO") //TODO

    override fun getObjectPath() = "/org/mpris/MediaPlayer2"

    override var canControl: Boolean by DBusProperty(true)
    override var canGoNext: Boolean by DBusProperty(true)
    override var canGoPrevious: Boolean by DBusProperty(true)
    override var canPlay: Boolean by DBusProperty(true)
    override var canPause: Boolean by DBusProperty(true)
    override var canSeek: Boolean by DBusProperty(true)
    override var playbackStatus: PlaybackStatus by DBusProperty(PlaybackStatus.Stopped)
    override var loopStatus: LoopStatus by DBusProperty(None) { repeat ->
        // TODO: this crashes for a ClassCastException
        cs.launch {
            playerController.changeQueue(
                playerController.queue?.copy(
                    repeatMode =
                    when (repeat) {
                        None -> DO_NOT_REPEAT
                        LoopStatus.Track -> REPEAT_SONG
                        Playlist -> REPEAT_QUEUE
                    }
                )
            )
        }
    }
    override var shuffle: Boolean by DBusProperty(false) { shuffled ->
        cs.launch {
            playerController.changeQueue(
                if (shuffled) {
                    playerController.queue?.shuffled()
                } else {
                    playerController.queue?.unshuffled()
                }
            )
        }
    }

    // TODO: handle set
    override var volume: Double by DBusProperty(1.0)
    override var position: Long by DBusProperty(0)
    override var metadata by DBusMapProperty(String::class, Variant::class, emptyMap())

    override var rate by DBusProperty(1.0)
    override val minimumRate by DBusConstant(1.0)
    override val maximumRate by DBusConstant(1.0)

    override fun Next() {
        cs.launch {
            val q = playerController.queue
            playerController.changeQueue(q?.next(), Position.Beginning)
        }
    }

    override fun Previous() {
        cs.launch {
            val q = playerController.queue
            playerController.changeQueue(q?.previous(), Position.Beginning)
        }
    }

    override fun Pause() {
        cs.launch {
            playerController.pause()
        }
    }

    override fun PlayPause() {
        cs.launch {
            if (playerController.pause) {
                playerController.play()
            } else {
                playerController.pause()
            }
        }
    }

    override fun Play() {
        cs.launch {
            playerController.play()
        }
    }

    override fun Stop() {
        cs.launch {
            playerController.changeQueue(null)
        }
    }

    override fun Seek(x: Long) {
        cs.launch {
            playerController.changeQueue(playerController.queue, Position.Specific(x.microseconds))
        }
    }

    override fun Raise() {
        //TODO
        println("Raise")
    }

    override fun Quit() {
        //TODO
        println("Quit")
    }

    override fun OpenUri(uri: String) {
        //TODO
        println("Open $uri")
    }
}