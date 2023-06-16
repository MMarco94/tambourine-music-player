package io.github.musicplayer.mpris

import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant
import org.mpris.MediaPlayer2.LoopStatus

class MPRISProperties(
    private var mprisState: MPRISState,
    private var mprisPlayerState: MPRISPlayerState,
    private val setFullscreen: (fullscreen: Boolean) -> Unit,
    private val setLoopStatus: (loopStatus: LoopStatus) -> Unit,
    private val setRate: (rate: Double) -> Unit,
    private val setShuffle: (shuffle: Boolean) -> Unit,
    private val setVolume: (volume: Double) -> Unit,
) : Properties {

    override fun getObjectPath() = "/org/mpris/MediaPlayer2"
    override fun isRemote() = false

    fun setState(mprisState: MPRISState): Properties.PropertiesChanged? {
        return if (this.mprisState != mprisState) {
            val diff = mprisState.diff(this.mprisState)
            this.mprisState = mprisState
            diff
        } else null
    }

    fun setPlayerState(mprisPlayerState: MPRISPlayerState): Properties.PropertiesChanged? {
        return if (this.mprisPlayerState != mprisPlayerState) {
            val diff = mprisPlayerState.diff(this.mprisPlayerState)
            this.mprisPlayerState = mprisPlayerState
            diff
        } else null
    }

    override fun GetAll(interfaceName: String): Map<String, Variant<*>> {
        return when (interfaceName) {
            MPRISState.interfaceName -> mprisState.variants
            MPRISPlayerState.interfaceName -> mprisPlayerState.variants
            else -> throw IllegalArgumentException("Unknown interface $interfaceName")
        }
    }

    override fun <A : Any> Get(interfaceName: String, propertyName: String): A {
        @Suppress("UNCHECKED_CAST")
        return GetAll(interfaceName).getValue(propertyName) as A
    }

    override fun <A : Any> Set(interfaceName: String, propertyName: String, value: A) {
        when (interfaceName) {
            MPRISState.interfaceName -> when (propertyName) {
                "Fullscreen" -> setFullscreen(value as Boolean)
                else -> throw IllegalArgumentException("Unknown property $propertyName for $interfaceName")
            }

            MPRISPlayerState.interfaceName -> when (propertyName) {
                "LoopStatus" -> setLoopStatus(LoopStatus.valueOf(value as String))
                "Rate" -> setRate(value as Double)
                "Shuffle" -> setShuffle(value as Boolean)
                "Volume" -> setVolume(value as Double)
                else -> throw IllegalArgumentException("Unknown property $propertyName for $interfaceName")
            }

            else -> throw IllegalArgumentException("Unknown interface $interfaceName")
        }
    }
}