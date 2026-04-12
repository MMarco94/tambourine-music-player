package io.github.mmarco94.tambourine.mpris

import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.audio.Position
import io.github.mmarco94.tambourine.data.RepeatMode.*
import io.github.mmarco94.tambourine.data.SongKey
import io.github.mmarco94.tambourine.utils.GLOBAL_CONNECTION
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.Properties
import org.mpris.MediaPlayer2.*
import org.mpris.MediaPlayer2.LoopStatus.None
import org.mpris.MediaPlayer2.LoopStatus.Playlist
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

private val logger = KotlinLogging.logger {}

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
            desktopEntry = "io.github.mmarco94.tambourine",
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
                playerController.transformQueue { queue ->
                    queue?.copy(
                        repeatMode = when (repeat) {
                            None -> DO_NOT_REPEAT
                            LoopStatus.Track -> REPEAT_SONG
                            Playlist -> REPEAT_QUEUE
                        }
                    ) to Position.Current
                }
            }
        },
        setShuffle = { shuffled ->
            cs.launch {
                playerController.transformQueue { queue ->
                    if (shuffled) {
                        queue?.shuffled()
                    } else {
                        queue?.unshuffled()
                    } to Position.Current
                }
            }
        },
        setFullscreen = { },
        setRate = { },
        setVolume = { level ->
            cs.launch {
                playerController.setLevel(level.toFloat())
            }
        },
    )
) : MediaPlayer2, MediaPlayer2Player, Properties by properties {

    private data class LastSentPositionData(val songKey: SongKey, val position: Duration, val eventTime: Instant)

    private var latestState = Channel<PlayerController.State>(Channel.CONFLATED)

    fun start() {
        cs.launch(Dispatchers.Default) {
            try {
                checkNotNull(GLOBAL_CONNECTION)
                GLOBAL_CONNECTION.exportObject("/org/mpris/MediaPlayer2", this@MPRISPlayerController)
                GLOBAL_CONNECTION.requestBusName("org.mpris.MediaPlayer2.io.github.mmarco94.tambourine")
                var prevEvent: LastSentPositionData? = null
                for (state in latestState) {
                    val newState = doSetState(GLOBAL_CONNECTION, prevEvent, state)
                    prevEvent = newState
                }
            } catch (e: Exception) {
                logger.error { "Error starting MPRIS: ${e.message}" }
            }
        }
    }

    suspend fun setState(state: PlayerController.State) {
        latestState.send(state)
    }

    private fun doSetState(
        connection: DBusConnection,
        lastPositionData: LastSentPositionData?,
        state: PlayerController.State,
    ): LastSentPositionData? {
        val currentSong = state.currentlyPlaying?.queue?.currentSong
        val position = state.calculateCurrentPosition(Clock.System.now())
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
            volume = state.level.toDouble(),
            position = position,
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
            if (lastPositionData != null && lastPositionData.songKey == state.currentlyPlaying?.queue?.currentSongKey) {
                val elapsed = now - lastPositionData.eventTime
                val expectedPosition = lastPositionData.position + elapsed
                val diff = (position - expectedPosition).absoluteValue
                diff < 250.milliseconds
            } else false
        val old = properties.mprisPlayerState
        properties.mprisPlayerState = mprisPlayerState
        val diff = mprisPlayerState.diff(old, skipPosition)
        if (diff != null) {
            try {
                connection.sendMessage(diff)
            } catch (e: Exception) {
                logger.error { "Cannot send message to the DBus: ${e.message}" }
            }
        }
        return if (skipPosition) {
            lastPositionData
        } else {
            if (state.currentlyPlaying != null) {
                LastSentPositionData(state.currentlyPlaying.queue.currentSongKey, position, now)
            } else {
                null
            }
        }
    }

    override fun Next() {
        cs.launch {
            playerController.transformQueue { queue ->
                queue?.next() to Position.Beginning
            }
        }
    }

    override fun Previous() {
        cs.launch {
            playerController.transformQueue { queue ->
                queue?.previous() to Position.Beginning
            }
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
            playerController.transformQueue { null to Position.Current }
        }
    }

    override fun Seek(x: Long) {
        cs.launch {
            playerController.transformQueue { queue ->
                if (queue != null) {
                    val position = playerController.position(Clock.System.now())
                    val pos = (position + x.microseconds).coerceAtLeast(ZERO)
                    if (pos > queue.currentSong.length) {
                        queue.next() to Position.Beginning
                    } else {
                        queue to Position.Specific(pos)
                    }
                } else {
                    null to Position.Current
                }
            }
        }
    }

    override fun SetPosition(trackId: TrackId, x: Long) {
        cs.launch {
            playerController.transformQueue { queue ->
                if (queue?.currentSongKey?.mprisTrackId() == trackId) {
                    queue to Position.Specific(x.microseconds)
                } else {
                    queue to Position.Current
                }
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