package io.github.mmarco94.tambourine.data

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.io.File
import java.nio.file.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

class LiveLibrary(
    private val scope: CoroutineScope,
    private val root: File
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchServiceMutex = Mutex()
    private val registeredKeys = mutableMapOf<File, WatchKey>()
    private val decoder = CoversDecoder(scope)
    private val pendingEvents = AtomicInteger(0)

    private var eventChannel = Channel<InternalEvent>()
    val channel = Channel<Library>(Channel.CONFLATED)

    suspend fun start() {
        suspendCancellableCoroutine<Unit> { cont ->
            scope.launch {
                pendingEvents.incrementAndGet()
                onNewFolder(root)
                while (true) {
                    val monitorKey = runInterruptible(Dispatchers.IO) {
                        watchService.take()
                    }
                    val dirPath = monitorKey.watchable() as Path
                    monitorKey.pollEvents().forEach { event ->
                        onFileEvent(dirPath, event)
                    }
                    if (!monitorKey.reset()) {
                        monitorKey.cancel()
                        break
                    }
                }
            }
            scope.launch {
                val rawMetadatas = mutableMapOf<File, RawMetadataSong>()
                while (true) {
                    when (val event = eventChannel.receive()) {
                        is InternalEvent.NewSong -> rawMetadatas[event.file] = event.metadata
                        is InternalEvent.FileDeleted -> {
                            rawMetadatas.keys.removeIf { song ->
                                song.path.startsWith(event.fileOrFolder.absolutePath)
                            }
                        }

                        is InternalEvent.FolderProcessed -> {}
                        is InternalEvent.FileIgnored -> {}
                    }
                    val remaining = pendingEvents.decrementAndGet()
                    if (remaining == 0) {
                        logger.info { "Processed ${rawMetadatas.size} songs" }
                        channel.send(Library.from(rawMetadatas.values))
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

    private fun onFileEvent(dirPath: Path, event: WatchEvent<*>) {
        val file = dirPath.resolve(event.context() as Path).toFile()
        pendingEvents.incrementAndGet()
        scope.launch {
            when (event.kind()) {
                StandardWatchEventKinds.ENTRY_CREATE -> onNew(file)
                StandardWatchEventKinds.ENTRY_DELETE -> onDeleted(file)
                else -> onModified(file)
            }
        }
    }

    private suspend fun onNew(fileOrFolder: File) {
        if (fileOrFolder.isDirectory) {
            onNewFolder(fileOrFolder)
        } else {
            onNewFile(fileOrFolder)
        }
    }

    private suspend fun onNewFolder(folder: File) {
        folder.listFiles()?.forEach { file ->
            pendingEvents.incrementAndGet()
            scope.launch {
                onNew(file)
            }
        }
        watchServiceMutex.withLock {
            registeredKeys.remove(folder)?.cancel()
            registeredKeys[folder] = folder.toPath().register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )
        }
        eventChannel.send(InternalEvent.FolderProcessed)
    }

    private suspend fun onNewFile(file: File) {
        try {
            eventChannel.send(
                InternalEvent.NewSong(
                    file,
                    RawMetadataSong.fromMusicFile(file, decoder)
                )
            )
        } catch (e: Exception) {
            eventChannel.send(InternalEvent.FileIgnored)
            logger.error("Error while parsing music file: ${e.message}")
        }
    }

    private suspend fun onDeleted(fileOrFolder: File) {
        watchServiceMutex.withLock {
            registeredKeys.remove(fileOrFolder)?.cancel()
        }
        eventChannel.send(InternalEvent.FileDeleted(fileOrFolder))
    }

    private suspend fun onModified(fileOrFolder: File) {
        if (fileOrFolder.isDirectory) {
            onModifiedFolder(fileOrFolder)
        } else {
            onModifiedFile(fileOrFolder)
        }
    }

    private suspend fun onModifiedFile(file: File) {
        onNewFile(file)
    }

    private suspend fun onModifiedFolder(folder: File) {
        eventChannel.send(InternalEvent.FolderProcessed)
    }


    private sealed interface InternalEvent {
        data class NewSong(
            val file: File,
            val metadata: RawMetadataSong,
        ) : InternalEvent

        data class FileDeleted(
            val fileOrFolder: File,
        ) : InternalEvent

        object FileIgnored : InternalEvent
        object FolderProcessed : InternalEvent
    }
}