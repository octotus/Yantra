package dev.yantra.app.engine

import android.content.Context
import java.io.File

class EphemerisAssets(private val context: Context) {
    fun install(): File {
        val directory = File(context.filesDir, "ephe")
        if (!directory.exists()) directory.mkdirs()
        copyIfNeeded("sepl_18.se1", directory)
        copyIfNeeded("semo_18.se1", directory)
        return directory
    }

    private fun copyIfNeeded(fileName: String, directory: File) {
        val destination = File(directory, fileName)
        if (destination.exists() && destination.length() > 0L) return

        context.assets.open("ephe/$fileName").use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
