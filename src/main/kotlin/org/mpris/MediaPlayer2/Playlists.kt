@file: Suppress("UNUSED")

package org.mpris.MediaPlayer2

import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal

typealias PlaylistId = DBusPath

/** [https://specifications.freedesktop.org/mpris-spec/latest/Playlists_Interface.html]
 *
 * ## Properties
 * ```
 * PlaylistCount   u                               Read only - The number of playlists available.
 * Orderings       as (Array of PlaylistOrdering)  Read only - The available orderings. At least one must be offered.
 * ActivePlaylist  (b(oss)) (MaybePlaylist)        Read only - The currently-active playlist.
 * ```
 * */
@DBusInterfaceName("org.mpris.MediaPlayer2.Playlists")
interface Playlists : DBusInterface {
    class PlaylistChanged(path: DBusPath, playlist: Playlist) : DBusSignal(path, playlist)

    fun ActivatePlaylist(playlist_id: DBusInterface)
    fun GetPlaylists(index: Int, max_count: Int, order: String, reverse_order: Boolean): List<Playlist>
}

/** # Playlist — (oss)
 * A data structure describing a playlist.
 * ## Properties
 * ```
 * Id   o - A unique identifier for the playlist. This should remain the same if the playlist is renamed.
 * Name s - The name of the playlist, typically given by the user.
 * Icon s - The URI of an (optional) icon.
 * ```
 * */
class Playlist @JvmOverloads
constructor(
    @field:Position(0)
    val id: PlaylistId,
    @field:Position(1)
    val name: String,
    @field:Position(2)
    val icon: Uri? = null
) : Struct()

/** # Maybe_Playlist — (b(oss))
 * A data structure describing a playlist, or nothing.
 * ## Properties
 * ```
 * Valid    b                - Whether this structure refers to a valid playlist.
 * Playlist (oss) (Playlist) - The playlist, providing Valid is true, otherwise undefined.
 * ```
 * */
class MaybePlaylist(
    @field:Position(0)
    val valid: Boolean,
    @field:Position(1)
    val playlist: Playlist
) : Struct() {
    /** Creates an empty MaybePlaylist. */
    constructor() : this(false, Playlist("/", ""))

    /** Creates a valid MaybePlaylist containing the given [playlist]. */
    constructor(playlist: Playlist) : this(true, playlist)
}

/** # Playlist_Ordering — s
 * Specifies the ordering of returned playlists. */
enum class PlaylistOrdering(private val value: String) : CharSequence by value {
    /** Alphabetical ordering by name, ascending. */
    Alphabetical("Alphabetical"),

    /** Ordering by creation date, oldest first. */
    CreationDate("Created"),

    /** Ordering by last modified date, oldest first. */
    ModifiedDate("Modified"),

    /** Ordering by date of last playback, oldest first. */
    LastPlayDate("Played"),

    /** A user-defined ordering. */
    UserDefined("User");
}