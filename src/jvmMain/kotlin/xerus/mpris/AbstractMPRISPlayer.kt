package xerus.mpris

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.types.Variant
import org.mpris.MediaPlayer2.*

/** Provides a typesafe foundation for implementing an MPRISPlayer.
 *
 * Every property inherited from an interface must either be null (if it is nullable and you don't want to implement it)
 * or delegated by a [DBusProperty].
 *
 * A val represents a Read-only field as declared by MPRIS, it is perfectly valid to implement it as var.
 * */
abstract class AbstractMPRISPlayer : MediaPlayerX, PlayerX, DefaultDBus {

	val connection: DBusConnection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION)
	val properties = HashMap<String, MutableMap<String, Variant<*>>>()
	internal val propertyListeners = HashMap<String, (Any) -> Unit>()

	override fun GetAll(interface_name: String) = properties[interface_name]

	override fun <A : Any> Set(interface_name: String, property_name: String, value: A) {
		super.Set(interface_name, property_name, value)
		propertyListeners[property_name]?.invoke(value)
	}

	/** Requests the bus name for this player and exports it, so that it can be called from DBus. */
	fun exportAs(playerName: String) {
		connection.exportObject("/org/mpris/MediaPlayer2", this)
		connection.requestBusName("org.mpris.MediaPlayer2.$playerName")
	}

	/** sends a [DBus.Properties.PropertiesChanged] signal via [connection] */
	override fun propertyChanged(interface_name: String, property_name: String) =
		super.propertyChanged(interface_name, property_name).also { connection.sendMessage(it) }

	override val hasTrackList by DBusConstant(this is TrackList)

}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface PlayerX : Player {
	/** Whether the media player may be controlled over this interface.
	 * Setting this to false assumes all properties as Read-only */
	val canControl: Boolean
	val canGoNext: Boolean
	val canGoPrevious: Boolean
	val canPlay: Boolean
	val canPause: Boolean
	val canSeek: Boolean

	val playbackStatus: PlaybackStatus

	/** _Optional_ */
	var loopStatus: LoopStatus

	/** A value of false indicates that playback is progressing linearly through a playlist,
	 * while true means playback is progressing through a playlist in some other order.
	 *
	 * _Optional_ */
	var shuffle: Boolean
	var volume: Double

	/** The current track position in microseconds, between 0 and the 'mpris:length' metadata entry (see [metadata]. */
	val position: Long

	/** Metadata of the current Track.
	 * [https://www.freedesktop.org/wiki/Specifications/mpris-spec/metadata/#index2h2] */
	val metadata: Map<String, Variant<*>>

	/** The current playback rate.
	 *
	 * The value must fall in the range described by [minimumRate] and [maximumRate], and must not be 0.0 */
	var rate: Double

	/** The minimum value which the [rate] property can take. */
	val minimumRate: Double

	/** The maximum value which the [rate] property can take. */
	val maximumRate: Double

}

/** Extension of the [MediaPlayer2] interface which adds its properties. */
@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayerX : MediaPlayer2 {
	val supportedUriSchemes: Array<String>
	val supportedMimeTypes: Array<String>

	/** Indicates whether this object implements the org.mpris.MediaPlayer2.TrackList interface. */
	val hasTrackList: Boolean

	val canRaise: Boolean
	val canQuit: Boolean

	/** _Optional_ */
	val canSetFullscreen: Boolean

	/** _Optional_ */
	var fullscreen: Boolean

	/** A friendly name to identify the media player to users.
	 * This should usually match the name found in the [desktopEntry]. */
	val identity: String

	/** The basename of an installed .desktop file which complies with the Desktop entry
	 * specification, with the ".desktop" extension stripped.
	 *
	 * _Optional_ */
	val desktopEntry: String
}

/** Extension of the [TrackList] interface which adds its properties. */
interface TrackListX : TrackList {
	/** If false, calling AddTrack or RemoveTrack will have no effect, and may raise a NotSupported error. */
	val canEditTracks: Boolean

	/** An array which contains the identifier of each track in this [TrackList], in order. */
	val tracks: Array<TrackId>
}

/** Extension of the [Playlists] interface which adds its properties. */
interface PlaylistsX : Playlists {
	val orderings: Array<PlaylistOrdering>
	val playlistCount: Int
	val activePlaylist: MaybePlaylist
}
