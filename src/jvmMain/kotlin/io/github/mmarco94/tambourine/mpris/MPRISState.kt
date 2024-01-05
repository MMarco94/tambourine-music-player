package io.github.mmarco94.tambourine.mpris

import io.github.mmarco94.tambourine.utils.diff
import io.github.mmarco94.tambourine.utils.variant
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant
import org.mpris.MediaPlayer2.LoopStatus
import org.mpris.MediaPlayer2.PlaybackStatus
import kotlin.time.Duration

// See https://specifications.freedesktop.org/mpris-spec/2.2/Media_Player.html
data class MPRISState(
    val canQuit: Boolean,
    val fullscreen: Boolean,
    val canSetFullscreen: Boolean,
    val canRaise: Boolean,
    val hasTrackList: Boolean,
    val identity: String,
    val desktopEntry: String,
    val supportedUriSchemes: List<String>,
    val supportedMimeTypes: List<String>,
) {

    companion object {
        const val interfaceName = "org.mpris.MediaPlayer2"
    }

    val variants: Map<String, Variant<*>> = mapOf(
        "CanQuit" to canQuit.variant(),
        "Fullscreen" to fullscreen.variant(),
        "CanSetFullscreen" to canSetFullscreen.variant(),
        "CanRaise" to canRaise.variant(),
        "HasTrackList" to hasTrackList.variant(),
        "Identity" to identity.variant(),
        "DesktopEntry" to desktopEntry.variant(),
        "SupportedUriSchemes" to supportedUriSchemes.variant(),
        "SupportedMimeTypes" to supportedMimeTypes.variant(),
    )
}


// See https://specifications.freedesktop.org/mpris-spec/2.2/Player_Interface.html
data class MPRISPlayerState(
    val playbackStatus: PlaybackStatus,
    val loopStatus: LoopStatus,
    val rate: Double,
    val shuffle: Boolean,
    val metadata: MPRISMetadata?,
    val volume: Double,
    val position: Duration,
    val minimumRate: Double,
    val maximumRate: Double,
    val canGoNext: Boolean,
    val canGoPrevious: Boolean,
    val canPlay: Boolean,
    val canPause: Boolean,
    val canSeek: Boolean,
    val canControl: Boolean,
) {

    companion object {
        const val objectPath = "/org/mpris/MediaPlayer2"
        const val interfaceName = "org.mpris.MediaPlayer2.Player"
    }

    val variants: Map<String, Variant<*>> = mapOf(
        "PlaybackStatus" to playbackStatus.variant(),
        "LoopStatus" to loopStatus.variant(),
        "Rate" to rate.variant(),
        "Shuffle" to shuffle.variant(),
        "Metadata" to (metadata?.variant ?: emptyMap<String, Variant<*>>().variant()),
        "Volume" to volume.variant(),
        "Position" to position.inWholeMicroseconds.variant(),
        "MinimumRate" to minimumRate.variant(),
        "MaximumRate" to maximumRate.variant(),
        "CanGoNext" to canGoNext.variant(),
        "CanGoPrevious" to canGoPrevious.variant(),
        "CanPlay" to canPlay.variant(),
        "CanPause" to canPause.variant(),
        "CanSeek" to canSeek.variant(),
        "CanControl" to canControl.variant(),
    )

    fun diff(old: MPRISPlayerState, skipPosition: Boolean): PropertiesChanged? {
        var diffs = variants.diff(old.variants)
        if (skipPosition) {
            diffs = diffs.minus("Position")
        }
        return if (diffs.isEmpty()) {
            null
        } else {
            PropertiesChanged(objectPath, interfaceName, diffs, emptyList())
        }
    }
}
