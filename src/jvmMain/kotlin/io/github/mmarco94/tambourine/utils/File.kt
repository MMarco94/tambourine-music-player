package io.github.mmarco94.tambourine.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.nfd.NativeFileDialog
import java.io.File


suspend fun nativeFileChooser(
    pickFiles: Boolean,
    initialDirectory: File? = null,
): File? = withContext(Dispatchers.IO) {
    Configuration.NFD_LINUX_PORTAL.set(true)
    val id = initialDirectory?.absolutePath.orEmpty()
    require(NativeFileDialog.NFD_Init() == NativeFileDialog.NFD_OKAY)
    try {
        MemoryStack.stackPush().use {
            val pathPointer = it.mallocPointer(1)
            val code = if (pickFiles) {
                // With a bit of will, this also supports file filters, see
                // https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/util/nfd/HelloNFD.java#L115
                NativeFileDialog.NFD_OpenDialog(pathPointer, null, id)
            } else {
                NativeFileDialog.NFD_PickFolder(pathPointer, id)
            }

            when (code) {
                NativeFileDialog.NFD_OKAY -> {
                    val path = pathPointer.stringUTF8
                    NativeFileDialog.NFD_FreePath(pathPointer[0])
                    File(path)
                }

                NativeFileDialog.NFD_CANCEL -> null
                NativeFileDialog.NFD_ERROR -> error("An error occurred while executing NativeFileDialog.NFD_PickFolder")
                else -> error("Unknown return code '${code}' from NativeFileDialog.NFD_PickFolder")
            }
        }
    } finally {
        NativeFileDialog.NFD_Quit()
    }
}
