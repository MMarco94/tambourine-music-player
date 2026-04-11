package io.github.mmarco94.tambourine.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import io.github.mmarco94.tambourine.ui.TambourineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import java.nio.file.Path
import java.util.prefs.Preferences
import kotlin.io.path.pathString

object Preferences {

    private val prefs: Preferences by lazy {
        Preferences.userRoot().node("/io/github/music-player")
    }
    val libraryFolder = PreferenceContainer(
        read = { prefs -> Path.of(prefs.get("library_folder", System.getProperty("user.home") + "/Music")) },
        write = { prefs, value ->
            prefs.put("library_folder", value.pathString)
        }
    )
    val useSystemDecorations = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("system_decorations", false) },
        write = { prefs, value ->
            prefs.putBoolean("system_decorations", value)
        }
    )
    val fontScale = PreferenceContainer(
        read = { prefs -> prefs.getFloat("font_scale", 1f).coerceAtLeast(0.1f) },
        write = { prefs, value ->
            prefs.putFloat("font_scale", value)
        }
    )
    val theme = PreferenceContainer(
        read = { prefs ->
            val preference = prefs.get("theme", "")
            TambourineTheme.UserPreference.entries.singleOrNull {
                it.name.equals(preference, ignoreCase = true)
            } ?: TambourineTheme.UserPreference.AUTO
        },
        write = { prefs, value ->
            prefs.put("theme", value.name)
        }
    )

    class PreferenceContainer<T>(
        val read: (Preferences) -> T,
        val write: (Preferences, T) -> Unit,
    ) {
        val signal = MutableStateFlow(Any())

        @Suppress("OPT_IN_USAGE")
        val flow: Flow<T>
            get() = signal
                .mapLatest { get() }
                .flowOn(Dispatchers.Default)

        val state: State<T>
            @OptIn(ExperimentalCoroutinesApi::class)
            @Composable
            get() {
                return remember {
                    signal.mapLatest { get() }
                }.collectAsState(initial = get())
            }

        fun reload() {
            signal.value = Any()
        }

        fun get(): T {
            return read(prefs)
        }

        fun set(value: T) {
            write(prefs, value)
            prefs.flush()
            reload()
        }
    }
}