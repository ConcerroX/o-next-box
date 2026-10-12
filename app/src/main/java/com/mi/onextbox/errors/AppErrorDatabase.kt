package com.mi.onextbox.errors

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class AppErrorDatabase(context: Context, name: String = NAME) : SQLiteOpenHelper(context, name, null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE errors (id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER NOT NULL, package TEXT NOT NULL, process TEXT NOT NULL, uid INTEGER NOT NULL, pid INTEGER NOT NULL, kind TEXT NOT NULL, summary TEXT NOT NULL, detail TEXT NOT NULL, truncated INTEGER NOT NULL, app_name TEXT NOT NULL, version TEXT NOT NULL, metadata TEXT NOT NULL DEFAULT '{}')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE errors ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}'")
    }

    fun append(record: AppErrorRecord): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val id = db.insertOrThrow("errors", null, ContentValues().apply {
                put("time", record.time)
                put("package", record.packageName)
                put("process", record.processName)
                put("uid", record.uid)
                put("pid", record.pid)
                put("kind", record.kind.name)
                put("summary", record.summary)
                put("detail", record.detail)
                put("truncated", if (record.truncated) 1 else 0)
                put("app_name", record.appName)
                put("version", record.version)
                put("metadata", AppErrorMetadataCodec.encode(record.metadata))
            })
            db.execSQL("DELETE FROM errors WHERE id NOT IN (SELECT id FROM errors ORDER BY id DESC LIMIT ${AppErrorRules.MAX_RECORDS})")
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun list(): Cursor = readableDatabase.rawQuery(
        "SELECT id,time,package,process,uid,pid,kind,summary,truncated,app_name,version FROM errors ORDER BY id DESC LIMIT ${AppErrorRules.MAX_RECORDS}", null,
    )

    fun detail(id: Long): AppErrorRecord? = readableDatabase.rawQuery(
        "SELECT id,time,package,process,uid,pid,kind,summary,truncated,app_name,version,detail,metadata FROM errors WHERE id=?", arrayOf(id.toString()),
    ).use { if (it.moveToFirst()) it.readRecord(withDetail = true) else null }

    fun clear(): Int = writableDatabase.delete("errors", null, null)
    fun remove(id: Long): Int = writableDatabase.delete("errors", "id=?", arrayOf(id.toString()))

    companion object {
        const val NAME = "app_errors.db"
    }
}

internal fun Cursor.readRecord(withDetail: Boolean = false) = AppErrorRecord(
    id = getLong(0), time = getLong(1), packageName = getString(2), processName = getString(3),
    uid = getInt(4), pid = getInt(5), kind = AppErrorKind.valueOf(getString(6)), summary = getString(7),
    truncated = getInt(8) != 0, appName = getString(9), version = getString(10), detail = if (withDetail) getString(11) else "",
    metadata = if (withDetail) AppErrorMetadataCodec.decode(getString(12)) else AppErrorMetadata(),
)
