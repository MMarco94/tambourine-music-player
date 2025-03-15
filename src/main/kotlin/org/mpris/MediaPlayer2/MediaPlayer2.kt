package org.mpris.MediaPlayer2

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface

/** [https://specifications.freedesktop.org/mpris-spec/latest/Media_Player.html]
 *
 * ## Properties
 * - CanQuit		        b	Read only
 * - Fullscreen		        b	Read/Write	(optional)
 * - CanSetFullscreen		b	Read only	(optional)
 * - CanRaise		        b	Read only
 * - HasTrackList		    b	Read only
 * - Identity		        s	Read only
 * - DesktopEntry		    s	Read only	(optional)
 * - SupportedUriSchemes	as	Read only
 * - SupportedMimeTypes	    as	Read only
 * */
@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayer2 : DBusInterface {
    /** Brings the media player's user interface to the front using any appropriate mechanism available.
     *
     * The media player may be unable to control how its user interface is displayed, or it may not have a graphical user interface at all.
     * In this case, the CanRaise property is false and this method does nothing. */
    fun Raise()

    /** Causes the media player to stop running.
     *
     * The media player may refuse to allow clients to shut it down.
     * In this case, the CanQuit property is false and this method does nothing.*/
    fun Quit()
}