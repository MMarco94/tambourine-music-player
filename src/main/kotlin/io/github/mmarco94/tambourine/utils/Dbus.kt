package io.github.mmarco94.tambourine.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.types.DBusListType
import org.freedesktop.dbus.types.DBusMapType
import org.freedesktop.dbus.types.Variant

private val logger = KotlinLogging.logger {}

val GLOBAL_CONNECTION = CompletableDeferred<DBusConnection?>()

suspend fun loadDbusCollection() {
    withContext(Dispatchers.Default) {
        val dbus = try {
            DBusConnectionBuilder.forSessionBus().apply {
                withShared(false)
                receivingThreadConfig().apply {
                    this.withSignalThreadCount(1)
                    this.withErrorHandlerThreadCount(1)
                    this.withMethodCallThreadCount(1)
                    this.withMethodReturnThreadCount(1)
                }
            }.build()
        } catch (e: Exception) {
            logger.error { "Error starting MPRIS: ${e.message}" }
            null
        }
        GLOBAL_CONNECTION.complete(dbus)
    }
}

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

fun Map<String, Variant<*>>.variant() = Variant(
    this,
    DBusMapType(String::class.java, Variant::class.java)
)

@JvmName("listVariant")
fun List<String>.variant() = Variant(
    this,
    DBusListType(String::class.java)
)
