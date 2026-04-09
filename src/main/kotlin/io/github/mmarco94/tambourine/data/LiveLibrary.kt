package io.github.mmarco94.tambourine.data

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.bjoernpetersen.m3u.M3uParser
import net.bjoernpetersen.m3u.model.M3uEntry
import java.nio.file.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

class LiveLibrary(
    private val scope: CoroutineScope,
    private val roots: Set<Path>
) {
    private val creationTime = TimeSource.Monotonic.markNow()
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchServiceMutex = Mutex()
    private val registeredKeys = mutableMapOf<Path, WatchKey>()
    private val pendingEvents = AtomicInteger(0)

    private var eventChannel = Channel<InternalEvent>()

    suspend fun start(flowCollector: FlowCollector<Library>) {
        suspendCancellableCoroutine<Unit> { cont ->
            scope.launch {
                logger.info { "Started song loading" }
                roots.forEach { root ->
                    onNew(root, decoder = CoversDecoder(scope))
                }
                while (true) {
                    val monitorKey = runInterruptible(Dispatchers.IO) {
                        watchService.take()
                    }
                    val dirPath = monitorKey.watchable() as Path
                    monitorKey.pollEvents().forEach { event ->
                        onFileEvent(dirPath, event, decoder = CoversDecoder(scope))
                    }
                    if (!monitorKey.reset()) {
                        monitorKey.cancel()
                        break
                    }
                }
            }
            scope.launch {
                val rawMetadatas = mutableMapOf<Path, RawMetadataSong>()
                val rawPlaylists = mutableMapOf<Path, List<M3uEntry>>()
                while (true) {
                    when (val event = eventChannel.receive()) {
                        is InternalEvent.NewSong -> rawMetadatas[event.file] = event.metadata
                        is InternalEvent.NewPlaylist -> rawPlaylists[event.file] = event.playlist
                        is InternalEvent.FileDeleted -> {
                            rawMetadatas.keys.removeIf { song ->
                                song.startsWith(event.fileOrFolder)
                            }
                            rawPlaylists.keys.removeIf { song ->
                                song.startsWith(event.fileOrFolder)
                            }
                        }

                        is InternalEvent.FolderProcessed -> {}
                        is InternalEvent.FileIgnored -> {}
                    }
                    val remaining = pendingEvents.decrementAndGet()
                    if (remaining == 0) {
                        val library = Library.from(rawMetadatas.values, rawPlaylists.entries)
                        System.gc()
                        logger.info {
                            val diff = creationTime.elapsedNow()
                            "Processed ${rawMetadatas.size} songs, ${rawPlaylists.size} playlists ($diff since beginning)"
                        }
                        flowCollector.emit(library)
                    }
                }
            }
            cont.invokeOnCancellation {
                scope.launch {
                    watchServiceMutex.withLock {
                        registeredKeys.values.forEach {
                            it.cancel()
                        }
                        registeredKeys.clear()
                    }
                }
            }
        }
    }

    private fun onFileEvent(dirPath: Path, event: WatchEvent<*>, decoder: CoversDecoder) {
        val file = dirPath.resolve(event.context() as Path)
        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> onNew(file, decoder)
            StandardWatchEventKinds.ENTRY_DELETE -> onDeleted(file)
            else -> onModified(file, decoder)
        }
    }

    private fun onNew(fileOrFolder: Path, decoder: CoversDecoder) {
        pendingEvents.incrementAndGet()
        scope.launch {
            if (fileOrFolder.isDirectory()) {
                onNewFolder(fileOrFolder, decoder)
            } else {
                onNewFile(fileOrFolder, decoder)
            }
        }
    }

    /** Warning: increment pendingEvents before calling this! */
    private suspend fun onNewFolder(folder: Path, decoder: CoversDecoder) {
        withContext(Dispatchers.IO) {
            Files.newDirectoryStream(folder).use { stream ->
                stream.forEach { file ->
                    onNew(file, decoder)
                }
            }
        }
        watchServiceMutex.withLock {
            registeredKeys.remove(folder)?.cancel()
            if (folder.exists()) {
                registeredKeys[folder] = folder.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
                )
            }
        }
        eventChannel.send(InternalEvent.FolderProcessed)
    }

    /** Warning: increment pendingEvents before calling this! */
    private suspend fun onNewFile(file: Path, decoder: CoversDecoder) {
        if (file.extension == "m3u") {
            onNewPlaylist(file)
        } else {
            onNewSong(file, decoder)
        }
    }

    private suspend fun onNewSong(file: Path, decoder: CoversDecoder) {
        try {
            val songInfo = RawMetadataSong.fromMusicFile(file, decoder)
            eventChannel.send(
                InternalEvent.NewSong(file, songInfo)
            )
        } catch (e: Exception) {
            eventChannel.send(InternalEvent.FileIgnored)
            logger.error {
                "Error while parsing music file $file: ${e.message}"
            }
        }
    }

    private suspend fun onNewPlaylist(file: Path) {
        try {
            val playlistInfo = M3uParser.parse(file)
            eventChannel.send(
                InternalEvent.NewPlaylist(file, playlistInfo)
            )
        } catch (e: Exception) {
            eventChannel.send(InternalEvent.FileIgnored)
            logger.error {
                "Error while parsing playlist file $file: ${e.message}"
            }
        }
    }

    private fun onDeleted(fileOrFolder: Path) {
        pendingEvents.incrementAndGet()
        scope.launch {
            watchServiceMutex.withLock {
                registeredKeys.remove(fileOrFolder)?.cancel()
            }
            eventChannel.send(InternalEvent.FileDeleted(fileOrFolder))
        }
    }

    private fun onModified(fileOrFolder: Path, decoder: CoversDecoder) {
        onNew(fileOrFolder, decoder)
    }

    private sealed interface InternalEvent {
        data class NewSong(
            val file: Path,
            val metadata: RawMetadataSong,
        ) : InternalEvent

        data class NewPlaylist(
            val file: Path,
            val playlist: List<M3uEntry>,
        ) : InternalEvent

        data class FileDeleted(
            val fileOrFolder: Path,
        ) : InternalEvent

        object FileIgnored : InternalEvent
        object FolderProcessed : InternalEvent
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<Set<Path>>.toLibrary(): Flow<Library?> {
    return transformLatest { roots ->
        emit(null)
        withContext(Dispatchers.Default) {
            val liveLibrary = LiveLibrary(this, roots)
            liveLibrary.start(this@transformLatest)
        }
    }
}