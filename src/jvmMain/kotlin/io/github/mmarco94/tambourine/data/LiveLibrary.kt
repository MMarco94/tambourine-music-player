package io.github.mmarco94.tambourine.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.io.File
import java.nio.file.*

private val logger = KotlinLogging.logger {}

class LiveLibrary(
    val scope: CoroutineScope,
    val root: File
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchServiceMutex = Mutex()
    private val registeredKeys = mutableMapOf<File, WatchKey>()
    private val decoder = CoversDecoder(scope)

    private var eventChannel = Channel<InternalEvent>()
    val channel = Channel<Library>(Channel.CONFLATED)

    suspend fun start() {
        channel.send(Library(emptyList(), emptyList(), emptyList()))
        scope.launch {
            onNewFolder(root)
            while (true) {
                val monitorKey = watchService.take()
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
        val rawMetadatas = mutableMapOf<File, RawMetadataSong>()
        while (true) {
            when (val event = eventChannel.receive()) {
                is InternalEvent.OnNewSong -> rawMetadatas[event.file] = event.metadata
                is InternalEvent.OnSongDeleted -> rawMetadatas.remove(event.file)
            }
            // Processing all queued events, without sending a new library
            while (true) {
                val maybeEvent = eventChannel.tryReceive()
                if (maybeEvent.isSuccess) {
                    when (val event = maybeEvent.getOrThrow()) {
                        is InternalEvent.OnNewSong -> rawMetadatas[event.file] = event.metadata
                        is InternalEvent.OnSongDeleted -> rawMetadatas.remove(event.file)
                    }
                } else break
            }
            logger.info { "Processed ${rawMetadatas.size} songs" }
            channel.send(Library.from(rawMetadatas.values))
        }
    }

    private fun onFileEvent(dirPath: Path, event: WatchEvent<*>) {
        val file = dirPath.resolve(event.context() as Path).toFile()
        scope.launch {
            when (event.kind()) {
                StandardWatchEventKinds.ENTRY_CREATE -> onNew(file)
                StandardWatchEventKinds.ENTRY_DELETE -> onDeleted(file)
                else -> onModified(file)
            }
        }
    }

    private suspend fun onNew(fileOrFolder: File) {
        if (fileOrFolder.isFile) {
            onNewFile(fileOrFolder)
        } else if (fileOrFolder.isDirectory) {
            onNewFolder(fileOrFolder)
        }
    }

    private suspend fun onNewFolder(folder: File) {
        folder.listFiles()?.forEach { file ->
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
    }

    private suspend fun onNewFile(file: File) {
        try {
            eventChannel.send(
                InternalEvent.OnNewSong(
                    file,
                    RawMetadataSong.fromMusicFile(file, decoder)
                )
            )
        } catch (e: Exception) {
            logger.error("Error while parsing music file: ${e.message}")
        }
    }

    private suspend fun onDeleted(fileOrFolder: File) {
        if (fileOrFolder.isFile) {
            onDeletedFile(fileOrFolder)
        } else if (fileOrFolder.isDirectory) {
            onDeletedFolder(fileOrFolder)
        }
    }

    private suspend fun onDeletedFolder(folder: File) {
        folder.listFiles()?.forEach { file ->
            scope.launch {
                onDeleted(file)
            }
        }
        watchServiceMutex.withLock {
            registeredKeys.remove(folder)?.cancel()
        }
    }

    private suspend fun onDeletedFile(file: File) {
        eventChannel.send(
            InternalEvent.OnSongDeleted(file)
        )
    }

    private suspend fun onModified(fileOrFolder: File) {
        if (fileOrFolder.isFile) {
            onModifiedFile(fileOrFolder)
        }
    }

    private suspend fun onModifiedFile(file: File) {
        onNewFile(file)
    }


    private sealed interface InternalEvent {
        data class OnNewSong(
            val file: File,
            val metadata: RawMetadataSong,
        ) : InternalEvent

        data class OnSongDeleted(
            val file: File,
        ) : InternalEvent
    }
}