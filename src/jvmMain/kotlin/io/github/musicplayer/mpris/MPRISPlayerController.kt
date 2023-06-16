package io.github.musicplayer.mpris

import io.github.musicplayer.audio.PlayerController
import io.github.musicplayer.audio.Position
import io.github.musicplayer.data.RepeatMode.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.Properties
import org.mpris.MediaPlayer2.*
import org.mpris.MediaPlayer2.LoopStatus.None
import org.mpris.MediaPlayer2.LoopStatus.Playlist
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.microseconds

class MPRISPlayerController(
    private val cs: CoroutineScope,
    private val playerController: PlayerController,
    private val properties: MPRISProperties = MPRISProperties(
        mprisState = MPRISState(
            canQuit = true,
            fullscreen = false,
            canSetFullscreen = false,
            canRaise = true,
            hasTrackList = false,
            identity = "Music Player",
            desktopEntry = "TODO",//TODO
            supportedUriSchemes = listOf("file"),
            supportedMimeTypes = listOf("audio/mpeg", "audio/mp4"),//TODO
        ),
        mprisPlayerState = MPRISPlayerState(
            playbackStatus = PlaybackStatus.Stopped,
            loopStatus = None,
            rate = 1.0,
            shuffle = false,
            metadata = null,
            volume = 1.0,
            position = ZERO,
            minimumRate = 1.0,
            maximumRate = 1.0,
            canGoNext = false,
            canGoPrevious = false,
            canPlay = false,
            canPause = false,
            canSeek = false,
            canControl = false,
        ),
        setLoopStatus = { repeat ->
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
        },
        setShuffle = { shuffled ->
            cs.launch {
                playerController.changeQueue(
                    if (shuffled) {
                        playerController.queue?.shuffled()
                    } else {
                        playerController.queue?.unshuffled()
                    }
                )
            }
        },
        setFullscreen = { },
        setRate = { },
        setVolume = { },
    )
) : MediaPlayer2, MediaPlayer2Player, Properties by properties {

    private val connection: DBusConnection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION)

    fun export() {
        connection.exportObject("/org/mpris/MediaPlayer2", this)
        connection.requestBusName("org.mpris.MediaPlayer2.music-player")
    }

    fun setState(state: PlayerController.State) {
        val currentSong = state.currentlyPlaying?.queue?.currentSong
        val diff = properties.setPlayerState(
            MPRISPlayerState(
                playbackStatus = if (state.currentlyPlaying == null) {
                    PlaybackStatus.Stopped
                } else if (state.pause) {
                    PlaybackStatus.Paused
                } else {
                    PlaybackStatus.Playing
                },
                loopStatus = if (state.currentlyPlaying == null) {
                    None
                } else when (state.currentlyPlaying.queue.repeatMode) {
                    DO_NOT_REPEAT -> None
                    REPEAT_QUEUE -> Playlist
                    REPEAT_SONG -> LoopStatus.Track
                },
                rate = 1.0,
                shuffle = state.currentlyPlaying != null && state.currentlyPlaying.queue.isShuffled,
                metadata = currentSong?.mprisMetadata(),
                volume = 1.0,
                position = state.position,
                minimumRate = 1.0,
                maximumRate = 1.0,
                canControl = state.currentlyPlaying != null,
                canGoNext = state.currentlyPlaying != null,
                canGoPrevious = state.currentlyPlaying != null,
                canPlay = state.currentlyPlaying != null,
                canPause = state.currentlyPlaying != null,
                canSeek = state.currentlyPlaying != null,
            )
        )
        if (diff != null) {
            connection.sendMessage(diff)
        }
    }

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
        // TODO: "If the value passed in would mean seeking beyond the end of the track, acts like a call to Next"
        cs.launch {
            val pos = (playerController.position + x.microseconds).coerceAtLeast(ZERO)
            playerController.changeQueue(playerController.queue, Position.Specific(pos))
        }
    }

    override fun SetPosition(trackId: TrackId, x: Long) {
        // TODO: "If this does not match the id of the currently-playing track, the call is ignored as "stale""
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