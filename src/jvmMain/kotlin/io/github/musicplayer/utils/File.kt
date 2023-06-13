package io.github.musicplayer.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

suspend fun readFolder(folder: File, onFile: suspend (File) -> Unit) {
    coroutineScope {
        readFolder(this, folder, onFile)
    }
}

private suspend fun readFolder(
    cs: CoroutineScope,
    folder: File,
    onFile: suspend (File) -> Unit,
) {
    folder.listFiles()?.forEach { file ->
        if (file.isFile) {
            onFile(file)
        } else if (file.isDirectory) {
            cs.launch {
                readFolder(cs, file, onFile)
            }
        }
    }
}