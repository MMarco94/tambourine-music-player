package io.github.musicplayer.utils

import org.freedesktop.dbus.types.DBusMapType
import org.freedesktop.dbus.types.Variant

fun String.variant(): Variant<*> {
    return Variant(this)
}

fun Long.variant(): Variant<*> {
    return Variant(this)
}

fun Double.variant(): Variant<*> {
    return Variant(this)
}

fun Boolean.variant(): Variant<*> {
    return Variant(this)
}

fun List<String>.variant(): Variant<*> {
    return Variant(toTypedArray())
}

fun Map<String, Variant<*>>.variant() = Variant(
    this,
    DBusMapType(String::class.java, Variant::class.java)
)
