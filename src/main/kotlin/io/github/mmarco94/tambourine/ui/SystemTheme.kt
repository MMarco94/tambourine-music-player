package io.github.mmarco94.tambourine.ui

import androidx.compose.runtime.*
import io.github.mmarco94.klibportal.portals.Settings
import io.github.mmarco94.klibportal.portals.appearanceSettingsFlow
import io.github.mmarco94.klibportal.portals.readAppearanceSettings
import io.github.mmarco94.tambourine.utils.GLOBAL_CONNECTION
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

private val defaultSystemAppearance = Settings.Appearance(
    colorScheme = Settings.Appearance.ColorScheme.NO_PREFERENCE,
    accentColor = null,
    contrast = Settings.Appearance.Contrast.NORMAL,
    reducedMotion = Settings.Appearance.ReducedMotion.NORMAL,
)

private val INITIAL_SYSTEM_THEME = CompletableDeferred<Settings.Appearance>()

suspend fun loadInitialSystemTheme() {
    val dbusCollection = GLOBAL_CONNECTION.await()
    val theme = if (dbusCollection == null) {
        defaultSystemAppearance
    } else {
        try {
            readAppearanceSettings(dbusCollection)
        } catch (e: Exception) {
            logger.error(e) { "Failed to read appearance" }
            defaultSystemAppearance
        }
    }
    INITIAL_SYSTEM_THEME.complete(theme)
}

@Composable
fun systemAppearanceSettings(): State<Settings.Appearance> {
    val dbusCollection = runBlocking { GLOBAL_CONNECTION.await() }
    if (dbusCollection == null) {
        return mutableStateOf(defaultSystemAppearance)
    }
    val flow = remember {
        flow {
            val dbusCollection = GLOBAL_CONNECTION.await()
            if (dbusCollection == null) {
                emit(defaultSystemAppearance)
            } else {
                emitAll(appearanceSettingsFlow(dbusCollection))
            }
        }
            .catch { e -> logger.error(e) { "Failed to read appearance" } }
            .flowOn(Dispatchers.IO)
    }
    return flow.collectAsState(remember {
        runBlocking { INITIAL_SYSTEM_THEME.await() }
    })
}
