package com.mi.onextbox.errors

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process

class AppErrorProvider : ContentProvider() {
    private lateinit var database: AppErrorDatabase
    override fun onCreate(): Boolean {
        database = AppErrorDatabase(requireNotNull(context))
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        requireOwner()
        require(uri == URI)
        return database.list().apply { setNotificationUri(requireNotNull(context).contentResolver, URI) }
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = requireNotNull(context)
        if (method == "append") {
            if (!AppErrorRules.canWrite(Binder.getCallingUid())) throw SecurityException("System UID required")
            if (!AppErrorTrackingSettings.isEnabled(ctx)) return Bundle.EMPTY
            val input = requireNotNull(extras).readError()
            val record = requireNotNull(AppErrorRules.bounded(input, Process.myUid() / 100_000))
            val id = database.append(record)
            ctx.contentResolver.notifyChange(URI, null)
            return Bundle().apply { putLong("id", id) }
        }
        requireOwner()
        return when (method) {
            "detail" -> database.detail(requireNotNull(arg?.toLongOrNull()).also { require(it > 0) })?.toBundle() ?: Bundle.EMPTY
            "clear" -> {
                database.clear()
                ctx.contentResolver.notifyChange(URI, null)
                Bundle.EMPTY
            }
            "remove" -> {
                database.remove(requireNotNull(arg?.toLongOrNull()).also { require(it > 0) })
                ctx.contentResolver.notifyChange(URI, null)
                Bundle.EMPTY
            }
            else -> throw IllegalArgumentException("Unknown error tracking operation")
        }
    }

    private fun requireOwner() {
        if (!AppErrorRules.canRead(Binder.getCallingUid(), Process.myUid())) throw SecurityException("Private error history")
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw SecurityException("Use the bounded error channel")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw SecurityException("Private error history")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw SecurityException("Private error history")

    companion object {
        val URI: Uri = Uri.parse("content://com.mi.onextbox.apperrors")
    }
}

internal fun AppErrorRecord.toBundle() = Bundle().apply {
    putLong("id", id)
    putLong("time", time)
    putString("package", packageName)
    putString("process", processName)
    putInt("uid", uid)
    putInt("pid", pid)
    putString("kind", kind.name)
    putString("summary", summary)
    putString("detail", detail)
    putBoolean("truncated", truncated)
    putString("app_name", appName)
    putString("version", version)
    putString("metadata", AppErrorMetadataCodec.encode(metadata))
}

internal fun Bundle.readError() = AppErrorRecord(
    id = getLong("id"), time = getLong("time"), packageName = requireNotNull(getString("package")),
    processName = requireNotNull(getString("process")), uid = getInt("uid", -1), pid = getInt("pid", -1),
    kind = AppErrorKind.valueOf(requireNotNull(getString("kind"))), summary = getString("summary", ""),
    detail = getString("detail", ""), truncated = getBoolean("truncated"),
    appName = getString("app_name", ""), version = getString("version", ""),
    metadata = AppErrorMetadataCodec.decode(getString("metadata")),
)
