package xerus.mpris

import org.freedesktop.dbus.types.Variant

/** Represents a Track with Metadata.
 * This is not an actual DBus-object, but rather a convenience wrapper around the metadata. */
interface Track {
    /** The TrackID, must be a valid DBus-Path. */
    val id: String
        get() = metadata.getValue("mpris:trackid").value as String

    /** Metadata of the Track. */
    val metadata: Map<String, Variant<*>>
}

class SimpleTrack(override val metadata: Map<String, Variant<*>>) : Track {
    constructor(createMetadata: PropertyMap.() -> Unit) : this(PropertyMap(createMetadata))
}
