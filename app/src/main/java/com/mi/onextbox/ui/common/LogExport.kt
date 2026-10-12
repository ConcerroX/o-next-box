package com.mi.onextbox.ui.common

import android.content.Context
import android.net.Uri
import java.io.File

/** Freeze an export before opening the document picker, without putting log text in saved state. */
internal object LogExport {
    fun prepare(context: Context, text: String): String {
        val file = File.createTempFile("onextbox-logs-", ".txt", context.cacheDir)
        try {
            file.writeText(text, Charsets.UTF_8)
            return file.absolutePath
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    fun save(context: Context, path: String, uri: Uri) {
        val file = ownedSnapshot(context, path)
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                file.inputStream().use { it.copyTo(output) }
            } ?: error("Unable to open the selected document")
        } finally {
            file.delete()
        }
    }

    fun discard(context: Context, path: String) {
        ownedSnapshot(context, path).delete()
    }

    private fun ownedSnapshot(context: Context, path: String): File {
        val file = File(path).canonicalFile
        require(file.parentFile == context.cacheDir.canonicalFile &&
            file.name.startsWith("onextbox-logs-") && file.extension == "txt") {
            "Invalid log snapshot"
        }
        return file
    }
}
