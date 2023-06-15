@file: Suppress("UNUSED")

package org.mpris.MediaPlayer2

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

typealias TrackId = String

/** [https://specifications.freedesktop.org/mpris-spec/latest/Track_List_Interface.html]
 *
 * ## Properties
 * - Tracks		    ao  Read only
 * - CanEditTracks	b   Read only
 * */
@DBusInterfaceName("org.mpris.MediaPlayer2.TrackList")
interface TrackList : DBusInterface {

    /** Indicates that the entire tracklist has been replaced.
     *
     * It is left up to the implementation to decide when a change to the track list is invasive enough that this signal should be emitted instead of a series of TrackAdded and TrackRemoved signals.
     *
     * @param path The path to the object this is emitted from.
     * @param tracks new content of the tracklist
     * @param currentTrack The identifier of the track to be considered as current.
     * `/org/mpris/MediaPlayer2/TrackList/NoTrack` indicates that there is no current track. */
    class TrackListReplaced(
        path: DBusPath,
        tracks: List<TrackId>,
        currentTrack: TrackId
    ) : DBusSignal(path, tracks, currentTrack)

    /** Indicates that a track has been added to the track list.
     *
     * @param path The path to the object this is emitted from.
     * @param metadata The metadata of the newly added item. This must include a mpris:trackid entry.
     * @param afterTrack The identifier of the track after which the new track was inserted.
     * The path `/org/mpris/MediaPlayer2/TrackList/NoTrack` indicates that the track was inserted at the start of the track list. */
    class TrackAdded(
        path: DBusPath,
        metadata: Map<String, Variant<*>>,
        afterTrack: TrackId
    ) : DBusSignal(path, metadata, afterTrack)

    /** Indicates that a track has been removed from the track list.
     *
     * @param path The path to the object this is emitted from.
     * @param trackId The identifier of the track being removed. */
    class TrackRemoved(
        path: DBusPath,
        trackId: TrackId
    ) : DBusSignal(path, trackId)

    /** Indicates that the metadata of a track in the tracklist has changed.
     *
     * This may indicate that a track has been replaced, in which case the mpris:trackid metadata entry is different from the TrackId argument.
     *
     * @param path The path to the object this is emitted from.
     * @param trackId The id of the track which metadata has changed. If the track id has changed, this will be the old value.
     * @param metadata metadata of the new track. This must include a mpris:trackid entry. */
    class TrackMetadataChanged(
        path: DBusPath,
        trackId: TrackId,
        metadata: Map<String, Variant<*>>
    ) : DBusSignal(path, trackId, metadata)

    fun GetTracksMetadata(trackIds: List<TrackId>): List<Map<String, Variant<*>>>

    fun AddTrack(uri: Uri, afterTrack: TrackId, setAsCurrent: Boolean)

    fun RemoveTrack(trackId: TrackId)

    fun GoTo(trackId: TrackId)

}