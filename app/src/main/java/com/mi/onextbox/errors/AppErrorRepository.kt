package com.mi.onextbox.errors

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

internal object AppErrorRepository {
    fun observe(context: Context) = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        context.contentResolver.registerContentObserver(AppErrorProvider.URI, false, observer)
        trySend(Unit)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.conflate().flowOn(Dispatchers.IO)

    fun list(context: Context): List<AppErrorRecord> = context.contentResolver.query(
        AppErrorProvider.URI, null, null, null, null,
    )?.use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.readRecord()) } } ?: error("Error history unavailable")

    fun detail(context: Context, id: Long): AppErrorRecord? =
        context.contentResolver.call(AppErrorProvider.URI, "detail", id.toString(), null)?.takeIf { it.containsKey("id") }?.readError()

    fun clear(context: Context) { context.contentResolver.call(AppErrorProvider.URI, "clear", null, null) }
    fun remove(context: Context, id: Long) { context.contentResolver.call(AppErrorProvider.URI, "remove", id.toString(), null) }
}
