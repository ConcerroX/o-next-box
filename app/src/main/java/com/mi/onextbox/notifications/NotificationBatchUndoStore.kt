package com.mi.onextbox.notifications

import android.content.Context
import android.util.AtomicFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object NotificationBatchUndoStore {
    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "notification-batch-undo.json"))

    @Synchronized
    fun load(context: Context): List<NotificationMutation> {
        val storage = file(context)
        if (!storage.baseFile.exists()) return emptyList()
        return runCatching {
            Json.decodeFromString<List<NotificationMutation>>(storage.openRead().use { it.reader().readText() })
                .takeIf { it.size <= 1500 && it.distinctBy { entry -> entry.target to entry.kind }.size == it.size }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(context: Context, mutations: List<NotificationMutation>) {
        val storage = file(context)
        val output = storage.startWrite()
        try {
            output.write(Json.encodeToString(mutations).toByteArray(Charsets.UTF_8))
            storage.finishWrite(output)
        } catch (error: Exception) {
            storage.failWrite(output)
            throw error
        }
    }
}
