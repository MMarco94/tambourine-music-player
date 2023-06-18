package io.github.mmarco94.tambourine.mpris

import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.audio.Position
import io.github.mmarco94.tambourine.data.RepeatMode.*
import io.github.mmarco94.tambourine.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.Properties
import org.mpris.MediaPlayer2.*
import org.mpris.MediaPlayer2.LoopStatus.None
import org.mpris.MediaPlayer2.LoopStatus.Playlist
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

class MPRISPlayerController(
    private val cs: CoroutineScope,
    private val playerController: PlayerController,
    private val quit: () -> Unit,
    private val raise: () -> Unit,
    private val properties: MPRISProperties = MPRISProperties(
        mprisState = MPRISState(
            canQuit = true,
            fullscreen = false,
            canSetFullscreen = false,
            canRaise = true,
            hasTrackList = false,
            identity = "Tambourine",
            desktopEntry = "TODO",//TODO
            supportedUriSchemes = listOf("file"),
            supportedMimeTypes = listOf("audio/mpeg", "audio/mp3"),//TODO
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

    private data class LastSentPositionData(val song: Song, val position: Duration, val eventTime: Instant)

    private val connection: DBusConnection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION)
    private var latestState = Channel<PlayerController.State>(Channel.CONFLATED)

    fun start() {
        connection.exportObject("/org/mpris/MediaPlayer2", this)
        connection.requestBusName("org.mpris.MediaPlayer2.tambourine-music-player")
        cs.launch(Dispatchers.Default) {
            var prevEvent: LastSentPositionData? = null
            for (state in latestState) {
                val newState = doSetState(prevEvent, state)
                prevEvent = newState
            }
        }
    }

    suspend fun setState(state: PlayerController.State) {
        latestState.send(state)
    }

    private fun doSetState(
        lastPositionData: LastSentPositionData?,
        state: PlayerController.State,
    ): LastSentPositionData? {
        val currentSong = state.currentlyPlaying?.queue?.currentSong
        val mprisPlayerState = MPRISPlayerState(
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
        // "Position" changes constantly. I need to be smart to know when to actually send the event
        val now = Clock.System.now()
        val skipPosition =
            if (lastPositionData != null && lastPositionData.song == state.currentlyPlaying?.queue?.currentSong) {
                val elapsed = now - lastPositionData.eventTime
                val expectedPosition = lastPositionData.position + elapsed
                val diff = (state.position - expectedPosition).absoluteValue
                diff < 250.milliseconds
            } else false
        val old = properties.mprisPlayerState
        properties.mprisPlayerState = mprisPlayerState
        val diff = mprisPlayerState.diff(old, skipPosition)
        if (diff != null) {
            connection.sendMessage(diff)
        }
        return if (skipPosition) {
            lastPositionData
        } else {
            if (state.currentlyPlaying != null) {
                LastSentPositionData(state.currentlyPlaying.queue.currentSong, state.position, now)
            } else {
                null
            }
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
        cs.launch {
            val current = playerController.queue
            if (current != null) {
                val pos = (playerController.position + x.microseconds).coerceAtLeast(ZERO)
                if (pos > current.currentSong.length) {
                    playerController.changeQueue(current.next(), Position.Beginning)
                } else {
                    playerController.changeQueue(current, Position.Specific(pos))
                }
            }
        }
    }

    override fun SetPosition(trackId: TrackId, x: Long) {
        cs.launch {
            if (playerController.queue?.currentSong?.mprisTrackId() == trackId) {
                playerController.changeQueue(playerController.queue, Position.Specific(x.microseconds))
            }
        }
    }

    override fun Raise() {
        raise()
    }

    override fun Quit() {
        quit()
    }

    override fun OpenUri(uri: String) {
        //TODO
        println("Open $uri")
    }
}