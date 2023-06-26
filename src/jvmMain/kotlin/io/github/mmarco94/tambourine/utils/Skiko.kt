package io.github.mmarco94.tambourine.utils

import org.jetbrains.skiko.Library
import org.jetbrains.skiko.hostId
import java.io.File
import java.nio.file.Files


fun skikoWorkaround731() {
    //Workaround for issue https://github.com/JetBrains/skiko/issues/731
    val libRoot = File(System.getProperty("java.home"), "lib")
    val resourceName = System.mapLibraryName("skiko-$hostId")
    val targetFile = libRoot.resolve(resourceName)

    if (!targetFile.exists()) {
        Library::class.java.getResourceAsStream("/$resourceName").use { input ->
            Files.copy(input!!, targetFile.toPath())
        }
    }
}